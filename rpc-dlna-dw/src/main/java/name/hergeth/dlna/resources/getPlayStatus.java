package name.hergeth.dlna.resources;

import java.util.concurrent.atomic.AtomicLong;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.codahale.metrics.annotation.Timed;

import name.hergeth.dlna.api.StringPlaylist;
import name.hergeth.dlna.core.DLNACtrl;
import name.hergeth.dlna.core.PlayJob;

@Path("/pstat")
@Produces(MediaType.APPLICATION_JSON)
public class getPlayStatus extends ResLogger {
	private final DLNACtrl dlnac;
	private final AtomicLong counter;

	public getPlayStatus(DLNACtrl c) {
		dlnac = c;
		this.counter = new AtomicLong();
	}

	@GET
	@Timed
	public StringPlaylist status() {

		PlayJob pj = dlnac.getJob();

		return new StringPlaylist(counter.incrementAndGet(), pj);
	}
}