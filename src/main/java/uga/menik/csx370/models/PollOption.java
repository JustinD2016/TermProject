package uga.menik.csx370.models;

public class PollOption {
    /**
     * Unique identifier for the poll option.
     */
    private final String optionId;

    /**
     * Text content of the poll option.
     */
    private final String optionText;

    /**
     * Number of votes received by the poll option.
     */
    private final int pollVoteCount;

    /**
     * Indicates whether the current user has voted for this option.
     */
    private final boolean hasUserVoted;

    /**
     * Indicates whether the poll is disabled based on whether the user has voted in it or not.
     */
    private final boolean pollDisabled;

    /**
     * Constructs a PollOption with specified details.
     *
     * @param optionId   the unique identifier of the poll option
     * @param optionText the text content of the poll option
     * @param pollVoteCount the number of votes received by the poll option
     * @param hasUserVoted indicates whether the current user has voted for this option
     * @param pollDisabled indicates whether the poll is disabled based on whether the user has voted in it or not
     */
    public PollOption(String optionId, String optionText, int pollVoteCount, boolean hasUserVoted, boolean pollDisabled) {
        this.optionId = optionId;
        this.optionText = optionText;
        this.pollVoteCount = pollVoteCount;
        this.hasUserVoted = hasUserVoted;
        this.pollDisabled = pollDisabled;
    }

    public String getOptionId() {
        return optionId;
    }

    public String getOptionText() {
        return optionText;
    }

    public int getPollVoteCount() {
        return pollVoteCount;
    }

    public boolean hasUserVoted() {
        return hasUserVoted;
    }

    public boolean isPollDisabled() {
        return pollDisabled;
    }
}
