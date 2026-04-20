package uga.menik.csx370.models;

import java.util.List;



public class Actor {
    
    private final String actorId;
    private final String firstName;
    private final String lastName;
    private final String profession;
    private final int birthYear;
    private final int deathYear;
    private final List<Title> titles;

    /**
     * Constructs an Actor object with the specified details.
     * @param actorId    the unique identifier for the actor
     * @param firstName  the first name of the actor
     * @param lastName   the last name of the actor
     * @param profession the profession of the actor (e.g., "Actor", "Director")
     * @param birthYear  the birth year of the actor
     * @param deathYear  the death year of the actor (use 0 if still alive)
     */
    public Actor(String actorId, String firstName, String lastName, String profession, int birthYear, int deathYear, List<Title> titles) {
        this.actorId = actorId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.profession = profession;
        this.birthYear = birthYear;
        this.deathYear = deathYear;
        this.titles = titles;
    }

    public String getActorId() {
        return actorId;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getProfession() {
        return profession;
    }

    public int getBirthYear() {
        return birthYear;
    }

    public int getDeathYear() {
        return deathYear;
    }

    public List<Title> getTitles() {
        return titles;
    }

}
