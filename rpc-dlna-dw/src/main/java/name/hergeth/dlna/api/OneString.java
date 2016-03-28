package name.hergeth.dlna.api;

import org.hibernate.validator.constraints.Length;

import com.fasterxml.jackson.annotation.JsonProperty;

public class OneString {
    private long id;

    @Length(max = 3)
    private String content;

    public OneString() {
        // Jackson deserialization
    }

    public OneString(long id, String content) {
        this.id = id;
        this.content = content;
    }

    @JsonProperty
    public long getId() {
        return id;
    }

    @JsonProperty
    public String getContent() {
        return content;
    }
}