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
        this.birthYearHint  = extractJson(hintResult, "birth_year");
        this.deathYearHint  = extractJson(hintResult, "death_year");
        this.professionHint = extractJson(hintResult, "profession");
        this.sharedTitles   = extractJsonArray(hintResult, "shared_titles");
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

