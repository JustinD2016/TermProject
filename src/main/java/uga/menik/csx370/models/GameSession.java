package uga.menik.csx370.models;

import java.util.List;

public class GameSession {
    private final String sessionId;
    private final String gameId;
    private final int guessesUsed;
    private final boolean solved;
    private final List<Guess> guesses;


    public GameSession(String sessionId, String userId, String gameId, int guessesUsed, boolean solved, List<Guess> guesses) {
        this.sessionId = sessionId;
        this.gameId = gameId;
        this.guessesUsed = guessesUsed;
        this.solved = solved;
        this.guesses = guesses;
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getGameId() {
        return gameId;
    }

    public int getGuessesUsed() {
        return guessesUsed;
    }

    public boolean isSolved() {
        return solved;
    }

    public List<Guess> getGuesses() {
        return guesses;
    }

}
