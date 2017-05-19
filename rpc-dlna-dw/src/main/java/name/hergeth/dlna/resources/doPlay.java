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

@Path("/play")
@Produces(MediaType.APPLICATION_JSON)
public class doPlay extends ResLogger {
	private final DLNACtrl dlnac;
	private final AtomicLong counter;

	public doPlay(DLNACtrl c) {
		dlnac = c;
		this.counter = new AtomicLong();
	}

	@GET
	@Timed
	public SimpleResult play( 
			@QueryParam("name") Optional<String> na, 
			@QueryParam("dest") Optional<String> de, 
			@QueryParam("src") Optional<String> sr,
			@QueryParam("itm") Optional<String> it, 
			@QueryParam("len") Optional<Integer> l, 
			@QueryParam("no") Optional<Integer> n,
			@QueryParam("loop") Optional<Boolean> loop,
			@QueryParam("random") Optional<Boolean> random
			) {
		jlog.info("Got play: name="+na
				+ " dest="+de
				+ " src="+sr 
				+ " itm="+it 
				+ " len="+l
				+ " no="+n
				+ " loop="+loop
				+ " random="+random);

		final String name = na.or("");
		final String dest = de.or("");
		final String src = sr.or("");
		final String itm = it.or("");
		final Integer no = n.or(0);
		final Integer len = l.or(0);
		final Boolean bLoop = loop.or(false);
		final Boolean bRandom = random.or(false);

		if(na.isPresent()){
			dlnac.play(name);
		}
		else if(de.isPresent() && sr.isPresent() && it.isPresent() && l.isPresent()){
			dlnac.play(dest, src, itm, len, no, bLoop, bRandom );
		}
		else{
			jlog.error("Not enough parameters in doPlay");
		}
		
		return new SimpleResult(counter.incrementAndGet(), "Play send!");
	}
}