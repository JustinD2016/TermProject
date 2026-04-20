package uga.menik.csx370.models;

public class Guess {
    private final int guessNumber;
    private final Actor actor;
    private final String hintResult;

    public Guess(int guessNumber, Actor actor, String hintResult) {
        this.guessNumber = guessNumber;
        this.actor = actor;
        this.hintResult = hintResult;
    }

    public int getGuessNumber() {
        return guessNumber;
    }

    public Actor getActor() {
        return actor;
    }

    public String getHintResult() {
        return hintResult;
    }
}
