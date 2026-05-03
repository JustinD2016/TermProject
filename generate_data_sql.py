import os
import pandas as pd
import pymysql


BATCH_SIZE = 500
MIN_VOTES = 50000
TARGET_ACTORS = 1500


def sql_val(value):
    if value is None or (isinstance(value, float) and pd.isna(value)):
        return "NULL"
    s = str(value).strip()
    return "NULL" if s in ("", "\\N") else pymysql.converters.escape_str(s)


def sql_int(value):
    if value is None or (isinstance(value, float) and pd.isna(value)):
        return "NULL"
    s = str(value).strip()
    if s in ("", "\\N"):
        return "NULL"
    try:
        return str(int(float(s)))
    except (ValueError, TypeError):
        return "NULL"


def split_name(name):
    if pd.isna(name) or not name or not name.strip():
        return ("", "")
    parts = name.strip().split(None, 1)
    return (parts[0], parts[1] if len(parts) > 1 else "")


def write_inserts(f, table, columns, rows):
    col_str = ", ".join(columns)
    for i in range(0, len(rows), BATCH_SIZE):
        batch = rows[i:i + BATCH_SIZE]
        f.write(f"INSERT IGNORE INTO {table} ({col_str}) VALUES\n")
        lines = [f"  ({', '.join(vals)})" for vals in batch]
        f.write(",\n".join(lines) + ";\n\n")


def generate():
    # Load ratings
    ratings = pd.read_csv("data/title_ratings.tsv", sep="\t", dtype={"tconst": str, "numVotes": int, "averageRating": float})
    popular = ratings[ratings["numVotes"] >= MIN_VOTES]
    popular_ids = set(popular["tconst"])
    votes_map = dict(zip(popular["tconst"], popular["numVotes"]))

    # Load titles, filter by type
    titles = pd.read_csv("data/titles.csv", dtype=str, na_values=["\\N"])
    allowed_types = {"movie", "tvseries", "tvmovie", "tvminiseries"}
    titles["titleType_lower"] = titles["titleType"].fillna("").str.strip().str.lower()
    titles = titles[titles["titleType_lower"].isin(allowed_types)]
    titles_full = titles.copy()
    valid_title_ids = set(titles[titles["tconst"].isin(popular_ids)]["tconst"])

    # Load and filter names to fit popular titles
    names = pd.read_csv("data/names.csv", dtype=str, na_values=["\\N"])
    names["first_profession"] = names["primaryProfession"].fillna("").str.split(",").str[0].str.strip().str.lower()
    names = names[
        names["first_profession"].isin(["actor", "actress"]) &
        names["birthYear"].notna() &
        names["knownForTitles"].notna() &
        (names["knownForTitles"].str.strip() != "")
    ]
    names["title_list"] = names["knownForTitles"].str.split(",").apply(lambda x: [t.strip() for t in x if t.strip()])
    names["popular_titles"] = names["title_list"].apply(lambda tl: [t for t in tl if t in valid_title_ids])
    names = names[names["popular_titles"].apply(len) > 0]

    # Score by total votes, take top 1500
    names["total_votes"] = names["popular_titles"].apply(lambda tl: sum(votes_map.get(t, 0) for t in tl))
    names = names.sort_values("total_votes", ascending=False).head(TARGET_ACTORS)

    # Filter titles_full to only those referenced by selected actors
    all_needed_ids = set(names["title_list"].explode())
    titles_full = titles_full[titles_full["tconst"].isin(all_needed_ids)]
    valid_title_ids = set(titles_full["tconst"])

    # Build actor_title pairs using explode + filter
    at_pairs = (
        names[["nconst", "title_list"]]
        .explode("title_list")
        .loc[lambda df: df["title_list"].isin(valid_title_ids)]
        .values.tolist()
    )

    # Write data.sql
    with open('data.sql', "w", encoding="utf-8") as f:
        f.write("USE actordle;\n\n")

        # Titles
        title_rows = [
            [sql_val(r["tconst"]), sql_val(r["titleType"]), sql_val(r["primaryTitle"]),
             sql_val(r["originalTitle"]), sql_int(r.get("isAdult", 0)), sql_int(r.get("startYear")),
             sql_int(r.get("endYear")), sql_int(r.get("runtimeMinutes")), sql_val(r.get("genres"))]
            for _, r in titles_full.iterrows()
        ]
        write_inserts(f, "title",
            ["title_id", "title_type", "primary_title", "original_title",
             "is_adult", "start_year", "end_year", "runtime_minutes", "genres"],
            title_rows)

        # Actors
        actor_rows = []
        for _, r in names.iterrows():
            first, last = split_name(r["primaryName"])
            actor_rows.append([
                sql_val(r["nconst"]), sql_val(first), sql_val(last),
                sql_int(r.get("birthYear")), sql_int(r.get("deathYear")),
                sql_val(r.get("primaryProfession"))
            ])
        write_inserts(f, "actor",
            ["actor_id", "first_name", "last_name", "birth_year", "death_year", "primary_profession"],
            actor_rows)

        # Actor-Title junction
        at_rows = [[sql_val(a), sql_val(t)] for a, t in at_pairs]
        write_inserts(f, "actor_title", ["actor_id", "title_id"], at_rows)

        # Daily game seed
        f.write("INSERT IGNORE INTO daily_game (game_date, actor_id)\n")
        f.write("SELECT CURDATE(), actor_id FROM actor ORDER BY RAND() LIMIT 1;\n")

    total = len(title_rows) + len(actor_rows) + len(at_rows)
    size_mb = os.path.getsize('data.sql') / (1024 * 1024)
    print(f"Done! {total:,} rows, {size_mb:.1f} MB")


if __name__ == "__main__":
    generate()