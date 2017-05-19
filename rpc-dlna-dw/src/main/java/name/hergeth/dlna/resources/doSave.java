package name.hergeth.dlna.resources;

import java.util.concurrent.atomic.AtomicLong;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.codahale.metrics.annotation.Timed;
import com.google.common.base.Optional;

import name.hergeth.dlna.api.SimpleResult;
import name.hergeth.dlna.core.DLNACtrl;

@Path("/save")
@Produces(MediaType.APPLICATION_JSON)
public class doSave extends ResLogger {
	private final DLNACtrl dlnac;
	private final AtomicLong counter;

	public doSave(DLNACtrl c) {
		dlnac = c;
		this.counter = new AtomicLong();
	}

	@GET
	@Timed
	public SimpleResult save( 
			@QueryParam("name") String name, 
			@QueryParam("dest") String dest, 
			@QueryParam("src") String src,
			@QueryParam("itm") String itm, 
			@QueryParam("len") int len, 
			@QueryParam("no") Optional<Integer> n,
			@QueryParam("loop") Optional<Boolean> loop,
			@QueryParam("random") Optional<Boolean> random
			) {
		jlog.info("Got save: name"+name
				+ " dest="+dest
				+ " src="+src 
				+ " itm="+itm 
				+ " len="+len
				+ " no="+n
				+ " loop="+loop
				+ " random="+random);

		final Integer no = n.or(0);
		final Boolean bLoop = loop.or(false);
		final Boolean bRandom = random.or(false);

		dlnac.saveJob(name, dest, src, itm, len, no, bLoop, bRandom );
		
		return new SimpleResult(counter.incrementAndGet(), "Save send!");
	}
}