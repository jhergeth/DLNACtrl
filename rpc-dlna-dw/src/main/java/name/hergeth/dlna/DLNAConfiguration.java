package name.hergeth.dlna;

import io.dropwizard.Configuration;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.hibernate.validator.constraints.NotEmpty;

public class DLNAConfiguration extends Configuration {
	@NotEmpty
	private String defaultTimeout;

	private int timeo;

	@JsonProperty
	public String getDefaultTimeout() {
		return defaultTimeout;
	}

	@JsonProperty
	public void setDefaultTimeout(String defaultTimeout) {
		this.defaultTimeout = defaultTimeout;
		timeo = Integer.parseInt(defaultTimeout);
	}

}