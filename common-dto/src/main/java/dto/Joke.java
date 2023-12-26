package dto;

import lombok.Data;

/**
 * @author nitesh
 */
@Data
public class Joke {

    private static final String JOKE_FORMAT = "Q: %s \nA: %s";

    private String setup;
    private String punchline;

    @Override
    public String toString() {
        return String.format(JOKE_FORMAT, this.setup, this.punchline);
    }
}