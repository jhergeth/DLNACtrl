package name.hergeth.dlna.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.hibernate.validator.constraints.Length;

public class StringArray {
    private long id;

    private String[] content;

    public StringArray() {
        // Jackson deserialization
    }

    public StringArray(long id, String[] content) {
        this.id = id;
        this.content = content;
    }

    @JsonProperty
    public long getId() {
        return id;
    }

    @JsonProperty
    public String[] getContent() {
        return content;
    }
}