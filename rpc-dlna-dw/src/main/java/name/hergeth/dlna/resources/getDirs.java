package name.hergeth.dlna.resources;

import java.util.concurrent.atomic.AtomicLong;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;

import org.fourthline.cling.model.meta.Device;
import org.fourthline.cling.support.model.container.Container;

import com.codahale.metrics.annotation.Timed;
import com.google.common.base.Optional;

import name.hergeth.dlna.api.DirCont;
import name.hergeth.dlna.api.IdName;
import name.hergeth.dlna.core.DLNACtrl;
import name.hergeth.dlna.core.DirContent;

@Path("/getDirs")
@Produces(MediaType.APPLICATION_JSON)
public class getDirs {
	private final DLNACtrl dlnac;
    private final AtomicLong counter;
    
    public getDirs(DLNACtrl c) {
    	dlnac = c;
        this.counter = new AtomicLong();
    }

//    @GET
//    @Timed
//    public StringArray getDirs(@QueryParam("devName") String devName, @QueryParam("from") String from) {
//
//        return new StringArray(counter.incrementAndGet(), dlnac.devBrowseDir(devName, from));
//    }
    @GET
    @Timed
    public DirCont getDirs(@QueryParam("name") String name, @QueryParam("itm") Optional<String> f) {
    	try{
	        final String from = f.or("0");
	
	    	Device dev = dlnac.findDeviceUID(name);
	    	
	    	if( dev == null ){
	    		throw new WebApplicationException("No device <"+name+"> found.");
	    	}
	    	
	    	DirContent dc = dlnac.getDirContent(dev, from);
	    	
    		return  new DirCont(counter.incrementAndGet(), dc);
    	}catch(Exception e){
    		throw new WebApplicationException(e.getMessage());
    	}
    }
}