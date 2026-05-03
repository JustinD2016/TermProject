package uga.menik.csx370.models;

import java.util.List;

/**
 * Represents an individual game played for the feed page. Which can either be the current logged in
 * user's or one of the users they follow. Contains all the information about the game needed to display
 * it on the feed page.
 */
public class Feed {
    /**
     * The username of the user who played the game.
     */
    private final String user;

    /**
     * The first name of the actor guessed.
     */
    private final String actorFirstName;

    /**
     * The last name of the actor guessed.
     */
    private final String actorLastName;

    /**
     * Whether the game was solved or not.
     */
    private final boolean solved;

    /**
     * The number of guesses used in the game.
     */
    private final int guessCount;

    /**
     * The time when the game was played.
     */
    private final String playedTime;

    /**
     * The list of guesses made in the game.
     */
    private final List<Guess> guesses;

    /**
     * The filename of the user's avatar image.
     */
    private final String avatarFileName;
    
    /**
     * Constructs a Feed object with the specified details.
     * @param user the username of the user who played the game
     * @param userId the user ID of the user who played the game (used to determine avatar)
     * @param actorFirstName the first name of the actor guessed
     * @param actorLastName the last name of the actor guessed
     * @param solved whether the game was solved or not
     * @param guessCount the number of guesses used in the game
     * @param playedTime the time when the game was played
     * @param guesses the list of guesses made in the game
     */
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
