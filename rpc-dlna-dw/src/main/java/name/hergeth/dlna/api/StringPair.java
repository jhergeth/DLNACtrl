package name.hergeth.dlna.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.hibernate.validator.constraints.Length;

public class StringPair {
    private long id;

    private String[][] content;

    public StringPair() {
        // Jackson deserialization
    }

    public StringPair(long id, String[][] content) {
        this.id = id;
        this.content = content;
    }

    @JsonProperty
    public long getId() {
        return id;
    }

    @JsonProperty
    public String[][] getContent() {
        return content;
    }
}