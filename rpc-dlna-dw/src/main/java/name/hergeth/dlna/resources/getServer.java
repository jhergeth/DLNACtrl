package name.hergeth.dlna.resources;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.codahale.metrics.annotation.Timed;

import name.hergeth.dlna.api.StringPair;
import name.hergeth.dlna.core.DLNACtrl;

@Path("/getServer")
@Produces(MediaType.APPLICATION_JSON)
public class getServer extends getDevice{

	public getServer(DLNACtrl c ) {
		super(c, "ContentDirectory");
    }

    @GET
    @Timed
    public StringPair getDevices() {
    	return super.getDevices();
    }
}