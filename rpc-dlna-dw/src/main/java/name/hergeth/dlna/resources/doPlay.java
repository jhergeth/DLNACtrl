package name.hergeth.dlna.resources;

import java.util.concurrent.atomic.AtomicLong;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.codahale.metrics.annotation.Timed;
import com.google.common.base.Optional;

import name.hergeth.dlna.api.OneString;
import name.hergeth.dlna.core.DLNACtrl;

@Path("/play")
@Produces(MediaType.APPLICATION_JSON)
public class doPlay {
	private final DLNACtrl dlnac;
    private final AtomicLong counter;

    public doPlay(DLNACtrl c) {
    	dlnac = c;
        this.counter = new AtomicLong();
    }

    @GET
    @Timed
    public OneString play(
    		@QueryParam("dest") String dest, 
    		@QueryParam("src") String src,
    		@QueryParam("itm") String itm,
    		@QueryParam("len") int len,
    		@QueryParam("no") Optional<Integer> n
    		
    		) {

    	final Integer no = n.or(1);

    	dlnac.play(dest, src, itm, len, no);
        return new OneString(counter.incrementAndGet(), "Play send!");
    }
}