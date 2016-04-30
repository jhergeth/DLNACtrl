package name.hergeth.dlna.resources;

import java.util.concurrent.atomic.AtomicLong;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;

import com.codahale.metrics.annotation.Timed;
import com.google.common.base.Optional;

import name.hergeth.dlna.api.SimpleResult;
import name.hergeth.dlna.core.DLNACtrl;

@Path("/cmd")
@Produces(MediaType.APPLICATION_JSON)
public class doCmd {
	private final DLNACtrl dlnac;
	private final AtomicLong counter;

	public doCmd(DLNACtrl c) {
		dlnac = c;
		this.counter = new AtomicLong();
	}

	@GET
	@Timed
	public SimpleResult cmd(@QueryParam("do") String cm, @QueryParam("no") Optional<Integer> n) {
		switch (cm) {
		case "init":
			return new SimpleResult(counter.incrementAndGet(), dlnac.init() ? "yes" : "no");
		case "jump":
			final Integer val = n.or(1);

			if (val > 0)
				dlnac.jumpForward();
			else
				dlnac.jumpBack();

			return new SimpleResult(counter.incrementAndGet(), "Jumping " + val + " steps.");
		}
		throw new WebApplicationException("Unknown parameter value in 'do'.");
	}
}