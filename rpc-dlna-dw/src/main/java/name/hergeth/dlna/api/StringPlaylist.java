package name.hergeth.dlna.api;

import com.fasterxml.jackson.annotation.JsonProperty;

import name.hergeth.dlna.core.PlayJob;

import org.hibernate.validator.constraints.Length;

public class StringPlaylist {
    private long id;

    private PlayJob content;

    public StringPlaylist() {
        // Jackson deserialization
    }

    public StringPlaylist(long id, PlayJob content) {
        this.id = id;
        this.content = content;
    }

    @JsonProperty
    public long getId() {
        return id;
    }

    @JsonProperty
    public PlayJob getContent() {
        return content;
    }
}