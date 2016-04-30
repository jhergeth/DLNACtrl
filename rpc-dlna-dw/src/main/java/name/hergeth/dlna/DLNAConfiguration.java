package name.hergeth.dlna;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.dropwizard.Configuration;

public class DLNAConfiguration extends Configuration {
	private String defaultTimeout;

	@JsonProperty
	public String getDefaultTimeout() {
		return defaultTimeout;
	}

	@JsonProperty
	public void setDefaultTimeout(String defaultTimeout) {
		this.defaultTimeout = defaultTimeout;
	}

}