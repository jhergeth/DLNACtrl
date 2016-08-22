package name.hergeth.dlna.resources;

import java.util.concurrent.atomic.AtomicLong;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.annotation.Timed;
import com.google.common.base.Optional;

import name.hergeth.dlna.api.SimpleResult;
import name.hergeth.dlna.core.DLNACtrl;

@Path("/cmd")
@Produces(MediaType.APPLICATION_JSON)
public class doCmd extends ResLogger {
	private final DLNACtrl dlnac;
	private final AtomicLong counter;
    
	public doCmd(DLNACtrl c) {
		dlnac = c;
		this.counter = new AtomicLong();
	}

	@GET
	@Timed
	public SimpleResult cmd(@QueryParam("do") String cm, 
							@QueryParam("rend") Optional<String> r, 
							@QueryParam("no") Optional<Integer> n,
							@QueryParam("name") Optional<String> na) {
		
		jlog.info("Got cmd: " + cm + " rend=" + r + " n="+n);
		switch (cm) {
		case "init":
			return new SimpleResult(counter.incrementAndGet(), dlnac.init() ? "yes" : "no");
		case "jump":
			Integer val = n.or(1);
			String rend = r.or(""); 

			if (rend.length() > 0 && val > 0)
				dlnac.jumpForward(rend, val);
			else
				dlnac.jumpBack(rend, -val);

			return new SimpleResult(counter.incrementAndGet(), "Jumping " + val + " steps.");
		case "save":
			val = n.or(1);
			rend = r.or("");
			String name = na.or("null");

			jlog.info("Saving to "+val+" : " + name);
			
			return new SimpleResult(counter.incrementAndGet(), "Saving to " + val + ": " + name);
		}
		throw new WebApplicationException("Unknown command in 'do':" + cm);
	}
}