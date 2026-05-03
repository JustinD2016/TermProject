package uga.menik.csx370.models;

import java.util.List;

public class Feed {
    private final String user;
    private final String actorFirstName;
    private final String actorLastName;
    private final boolean solved;
    private final int guessCount;
    private final String playedTime;
    private final List<Guess> guesses;
    private final String avatarFileName;
    
    public Feed(String user, String userId, String actorFirstName, String actorLastName, boolean solved, int guessCount, String playedTime, List<Guess> guesses) {
        this.user = user;
        int fileNo = (Math.abs(userId.hashCode()) % 20) + 1;
        this.avatarFileName = String.format("/avatars/avatar_%d.png", fileNo);
        this.actorFirstName = actorFirstName;
        this.actorLastName = actorLastName;
        this.solved = solved;
        this.guessCount = guessCount;
        this.playedTime = playedTime;
        this.guesses = guesses;
    }

    public String getUser() {
        return user;
    }

    public String getAvatarFileName() {
        return avatarFileName;
    }

    public String getActorFirstName() {
        return actorFirstName;
    }

    public String getActorLastName() {
        return actorLastName;
    }

    public boolean getSolved() {
        return solved;
    }

    public int getGuessCount() {
        return guessCount;
    }

    public String getPlayedTime() {
        return playedTime;
    }

    public List<Guess> getGuesses() {
        return guesses;
    }
}
