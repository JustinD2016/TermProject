package uga.menik.csx370.models;

import java.util.List;

public class Guess {
    private final int guessNumber;
    private final Actor actor;
    private final String hintResult;
    private final String birthYearHint;
    private final String deathYearHint;
    private final String professionHint;
    private final String sharedTitles;

    public Guess(int guessNumber, Actor actor, String hintResult) {
        this.guessNumber = guessNumber;
        this.actor = actor;
        this.hintResult = hintResult;

        this.birthYearHint  = extractJson(hintResult, "birth_year");
        this.deathYearHint  = extractJson(hintResult, "death_year");
        this.professionHint = extractJson(hintResult, "profession");
        this.sharedTitles   = extractJson(hintResult, "shared_titles");
    }

    

    public int getGuessNumber() {
        return guessNumber;
    }

    public String getFirstName() {
        return actor.getFirstName();
    }

    public String getLastName() {
        return actor.getLastName();
    }

    public Actor getActor() {
        return actor;
    }

    public String getHintResult() {
        return hintResult;
    }

    public String getBirthYear() {
        System.out.println(this.birthYearHint);
        return this.birthYearHint;
    }

    public String getDeathYear() {
        if (this.deathYearHint.equals("N/A")||this.deathYearHint.equals("null")) {
            return "0"; // Use "0" to indicate still alive
        }
        return this.deathYearHint;
    }
    
    public String getProfession() {
        return this.professionHint;
    }

    public String getTitles() {
        return this.sharedTitles;
    }

    private String extractJson(String json, String key) {
        String search = "\"" + key + "\":\"";
        int start = json.indexOf(search);
        if (start == -1) return "";
        start += search.length();
        int end = json.indexOf("\"", start);
        return end == -1 ? "" : json.substring(start, end);
    }
}
