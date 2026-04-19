package uga.menik.csx370.models;

import java.util.List;
/**
 * Represents a poll post in the micro blogging platform,
 * extending the BasicPost with poll-specific features.
 */
public class Poll {
    /**
     * Unique identifier for the poll.
     */
    private final String pollId;

    /**
     * Text question of the poll.
     */
    private final String question;

    /**
     * Date when the poll was created.
     */
    private final String pollDate;

    /**
     * User who created the poll.
     */
    private final User user;

    /** *
     * List of options available in the poll.
     */
    private final List<PollOption> options;

    /**
     * Constructs a BasicPoll with specified details.
     *
     * @param pollId     the unique identifier of the poll
     * @param question   the text question of the poll
     * @param pollDate   the creation date of the poll
     * @param user       the user who created the poll
     * @param options    the list of options available in the poll
     */
    public Poll(String pollId, String question, String pollDate, User user, List<PollOption> options) {
        this.pollId = pollId;
        this.question = question;
        this.pollDate = pollDate;
        this.user = user;
        this.options = options;
    }

    public String getPollId() {
        return pollId;
    }

    public String getQuestion() {
        return question;
    }

    public String getPollDate() {
        return pollDate;
    }

    public User getUser() {
        return user;
    }

    public List<PollOption> getOptions() {
        return options;
    }

}
    

