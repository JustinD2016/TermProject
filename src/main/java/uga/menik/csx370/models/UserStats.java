package uga.menik.csx370.models;

public class UserStats {

    private final String username;
    private final int gamesPlayed;
    private final int gamesWon;
    private final double winRate;
    private final int currentStreak;
    private final int maxStreak;
    private final double avgGuesses;
    private final int rank;

    public UserStats(String username, int gamesPlayed, int gamesWon, double winRate, int currentStreak, int maxStreak, double avgGuesses, int rank) {
        this.username = username;
        this.gamesPlayed = gamesPlayed;
        this.gamesWon = gamesWon;
        this.winRate = winRate;
        this.currentStreak = currentStreak;
        this.maxStreak = maxStreak;
        this.avgGuesses = avgGuesses;
        this.rank = rank;
    }

    public String getUsername() {
        return username;
    }
    
    public int getGamesPlayed() {
        return gamesPlayed;
    }

    public int getGamesWon() {
        return gamesWon;
    }

    public double getWinRate() {
        return winRate;
    }

    public int getCurrentStreak() {
        return currentStreak;
    }

    public int getMaxStreak() {
        return maxStreak;
    }

    public double getAvgGuesses() {
        return avgGuesses;
    }

    public int getRank() {
        return rank;
    }

}
