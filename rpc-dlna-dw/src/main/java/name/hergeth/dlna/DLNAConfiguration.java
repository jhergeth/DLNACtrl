package name.hergeth.dlna;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.dropwizard.Configuration;

public class DLNAConfiguration extends Configuration {
	private String defaultTimeout;
	private String defaultSavePath;

	@JsonProperty
	public String getDefaultTimeout() {
		return defaultTimeout;
	}

	@JsonProperty
	public void setDefaultTimeout(String defaultTimeout) {
		this.defaultTimeout = defaultTimeout;
	}

	@JsonProperty
	public String getDefaultSavePath() {
		return defaultSavePath;
	}

	@JsonProperty
	public void setDefaultSavePath(String defaultSavePath) {
		this.defaultSavePath = defaultSavePath;
	}

}