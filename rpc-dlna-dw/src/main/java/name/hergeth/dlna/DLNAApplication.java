package name.hergeth.dlna;

import java.util.concurrent.ExecutorService;

import io.dropwizard.Application;
import io.dropwizard.lifecycle.setup.ExecutorServiceBuilder;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import name.hergeth.dlna.core.DLNACtrl;
import name.hergeth.dlna.health.TemplateHealthCheck;
import name.hergeth.dlna.resources.doCmd;
import name.hergeth.dlna.resources.doPlay;
import name.hergeth.dlna.resources.getDirs;
import name.hergeth.dlna.resources.getPlayStatus;
import name.hergeth.dlna.resources.getRenderer;
import name.hergeth.dlna.resources.getServer;

public class DLNAApplication extends Application<DLNAConfiguration> {
	static private DLNAConfiguration config = null;
	private DLNACtrl dlnac;

	public static void main(String[] args) throws Exception {
		new DLNAApplication().run(args);
	}
	
	public static DLNAConfiguration Config(){
		return config;
	}

	@Override
	public String getName() {
		return "DLNA";
	}

	@Override
	public void initialize(Bootstrap<DLNAConfiguration> bootstrap) {
		// nothing to do yet
	}

	@Override
	public void run(DLNAConfiguration configuration, Environment environment) {
		config = configuration;

		ExecutorServiceBuilder eb = environment.lifecycle().executorService(getName());
		ExecutorService esrv = eb.build();

		dlnac = new DLNACtrl(esrv);
		environment.lifecycle().manage(dlnac);

		final TemplateHealthCheck healthCheck = new TemplateHealthCheck("");
		environment.healthChecks().register("template", healthCheck);

		environment.jersey().register(new doCmd(dlnac));
		environment.jersey().register(new getRenderer(dlnac));
		environment.jersey().register(new getServer(dlnac));
		environment.jersey().register(new getDirs(dlnac));
		environment.jersey().register(new doPlay(dlnac));
		environment.jersey().register(new getPlayStatus(dlnac));
	}

}