package uga.menik.csx370.models;

import java.util.*;

public class Title {
    
    private final String titleId;
    private final String primaryName;
    private final String titleType;
    private final int releaseYear;
    private final List<String> genres;


    public Title(String titleId, String primaryName, String titleType, int releaseYear, List<String> genres) {
        this.titleId = titleId;
        this.primaryName = primaryName;
        this.titleType = titleType;
        this.releaseYear = releaseYear;
        this.genres = new ArrayList<>(genres);
    }

    public String getTitleId() {
        return titleId;
    }

    public String getPrimaryName() {
        return primaryName;
    }

    public String getTitleType() {
        return titleType;
    }

    public int getReleaseYear() {
        return releaseYear;
    }
    public List<String> getGenres() {
        return new ArrayList<>(genres);
    }
}
