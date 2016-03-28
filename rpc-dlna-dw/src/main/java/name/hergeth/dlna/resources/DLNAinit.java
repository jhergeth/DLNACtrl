package name.hergeth.dlna.resources;

import java.util.concurrent.atomic.AtomicLong;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.codahale.metrics.annotation.Timed;

import name.hergeth.dlna.DLNAApplication;
import name.hergeth.dlna.api.OneString;
import name.hergeth.dlna.core.DLNACtrl;

@Path("/init")
@Produces(MediaType.APPLICATION_JSON)
public class DLNAinit {
	private final DLNACtrl dlnac;
    private final AtomicLong counter;

    public DLNAinit(DLNACtrl c) {
    	dlnac = c;
        this.counter = new AtomicLong();
    }

    @GET
    @Timed
    public OneString init() {
        return new OneString(counter.incrementAndGet(), dlnac.isInit()?"yes":"no");
    }
}