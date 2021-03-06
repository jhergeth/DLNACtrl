package name.hergeth.dlna.resources;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.codahale.metrics.annotation.Timed;

import name.hergeth.dlna.api.IdName;
import name.hergeth.dlna.core.DLNACtrl;

@Path("/getRenderer")
@Produces(MediaType.APPLICATION_JSON)
public class getRenderer extends getDevice {

	public getRenderer(DLNACtrl c) {
		super(c, "AVTransport");
	}

	@GET
	@Timed
	public IdName getDevices() {
		return super.getDevices();
	}
}