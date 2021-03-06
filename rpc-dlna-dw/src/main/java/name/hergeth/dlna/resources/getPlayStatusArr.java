package name.hergeth.dlna.resources;

import java.util.concurrent.atomic.AtomicLong;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.codahale.metrics.annotation.Timed;

import name.hergeth.dlna.api.StringPlaylistArr;
import name.hergeth.dlna.core.DLNACtrl;

@Path("/pstatall")
@Produces(MediaType.APPLICATION_JSON)
public class getPlayStatusArr extends ResLogger {
	private final DLNACtrl dlnac;
	private final AtomicLong counter;

	public getPlayStatusArr(DLNACtrl c) {
		dlnac = c;
		this.counter = new AtomicLong();
	}

	@GET
	@Timed
	public StringPlaylistArr status() {
		return new StringPlaylistArr(counter.incrementAndGet(), dlnac.getJobs());
	}
}