package name.hergeth.dlna.api;

import com.fasterxml.jackson.annotation.JsonProperty;

import name.hergeth.dlna.core.PlayJob;

public class StringPlaylistArr {
	private long id;

	private PlayJob content[];

	public StringPlaylistArr() {
		// Jackson deserialization
	}

	public StringPlaylistArr(long id, PlayJob[] content) {
		this.id = id;
		this.content = content;
	}

	@JsonProperty
	public long getId() {
		return id;
	}

	@JsonProperty
	public PlayJob[] getContent() {
		return content;
	}
}