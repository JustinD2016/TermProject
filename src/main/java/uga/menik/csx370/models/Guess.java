package uga.menik.csx370.models;

import java.util.List;

public class Guess {
    private final int guessNumber;
    private final Actor actor;
    private final String hintResult; // keep raw if needed
    private final String birthYearHint;
    private final String deathYearHint;
    private final String professionHint;
    private final String sharedTitles;

    public Guess(int guessNumber, Actor actor, String hintResult) {
        this.guessNumber = guessNumber;
        this.actor = actor;
        this.hintResult = hintResult;

        // Parse the JSON string into individual fields
        String birthYear = extractJson(hintResult, "birth_year");
        int guesedBirthYear = actor.getBirthYear();
        if (birthYear.equals("higher")) {
            this.birthYearHint = "<span class=\"hint-incorrect\">&gt; " + guesedBirthYear + "</span>";
        } else if (birthYear.equals("lower")) {
            this.birthYearHint = "<span class=\"hint-incorrect\">&lt; " + guesedBirthYear + "</span>";
        } else if (birthYear.equals("match")) {
            this.birthYearHint = "<span class=\"hint-correct\">" + String.valueOf(guesedBirthYear) + "</span>";
        } else {
            this.birthYearHint = "Unknown";
        }

        String deathYear = extractJson(hintResult, "death_year");
        int guessedDeathYear = actor.getDeathYear();
        if (deathYear.equals("both_alive")) {
            this.deathYearHint = "<span class=\"hint-correct\">Alive</span>";
        } else if (deathYear.equals("higher")) {
            this.deathYearHint = "<span class=\"hint-incorrect\">&gt; " + guessedDeathYear + "</span>";
        } else if (deathYear.equals("lower")) {
            this.deathYearHint = "<span class=\"hint-incorrect\">&lt; " + guessedDeathYear + "</span>";
        } else if (deathYear.equals("match")) {
            this.deathYearHint = "<span class=\"hint-correct\">" + String.valueOf(guessedDeathYear) + "</span>";
        } else if (deathYear.equals("unknown") && guessedDeathYear <= 0) {
            this.deathYearHint = "<span class=\"hint-incorrect\">Deceased</span>";
        } else {
            this.deathYearHint = "<span class=\"hint-incorrect\">Still Alive</span>";
        }

        this.professionHint = createProfessionHTML(hintResult);

        String titles = extractJsonArray(hintResult, "shared_titles"); 
        this.sharedTitles = titles.isEmpty() || titles.equals("[]") ? "No Shared Titles"
            : titles.replaceAll("[\\[\\]\"]", "").replace(",", ", ").trim();
    }

    // Simple JSON field extractor — no library needed
    private String extractJson(String json, String key) {
        String search = "\"" + key + "\": \"";
        int start = json.indexOf(search);
        if (start == -1) return "";
        start += search.length();
        int end = json.indexOf("\"", start);
        return end == -1 ? "" : json.substring(start, end);
    }

    private String extractJsonArray(String json, String key) {
        String search = "\"" + key + "\": [";
        int start = json.indexOf(search);
        if (start == -1) return "";
        start += search.length() - 1; // include the [
        int end = json.indexOf("]", start);
        return end == -1 ? "" : json.substring(start, end + 1);
    }


    private String createProfessionHTML(String json) {
        StringBuilder html = new StringBuilder();

        String correct = extractJsonArray(json, "correct_professions");
        for (String profession : correct.replaceAll("[\\[\\]\"]", "").split(",")) {
            if (!profession.trim().isEmpty()) {
                html.append("<span class =\"proffesion-correct\">").append(profession.trim()).append("</span> ");
            }
        }
        String incorrect = extractJsonArray(json, "incorrect_professions");
        for (String profession : incorrect.replaceAll("[\\[\\]\"]", "").split(",")) {
            if (!profession.trim().isEmpty()) {
                html.append("<span class =\"proffesion-incorrect\">").append(profession.trim()).append("</span> ");
            }
        }
        return html.toString().trim();
    }

    public int getGuessNumber()    { return guessNumber; }
    public Actor getActor()        { return actor; }
    public String getHintResult()  { return hintResult; }
    public String getBirthYearHint()  { return birthYearHint; }
    public String getDeathYearHint()  { return deathYearHint; }
    public String getProfessionHint() { return professionHint; }
    public String getSharedTitles()   { return sharedTitles; }
    public String getFirstName()    { return actor.getFirstName(); }
    public String getLastName()     { return actor.getLastName(); }
}

