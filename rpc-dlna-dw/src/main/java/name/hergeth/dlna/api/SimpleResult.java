package name.hergeth.dlna.api;

import org.hibernate.validator.constraints.Length;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SimpleResult {
	private long id;

	@Length(max = 3)
	private String result;
	private String error;

	public SimpleResult() {
		// Jackson deserialization
	}

	public SimpleResult(long id, String r) {
		this.id = id;
		this.result = r;
		this.error = "";
	}

	public SimpleResult(long id, String r, String e) {
		this.id = id;
		this.result = r;
		this.error = e;
	}

	@JsonProperty
	public long getId() {
		return id;
	}

	@JsonProperty
	public String getResult() {
		return result;
	}

	@JsonProperty
	public String getError() {
		return error;
	}
}