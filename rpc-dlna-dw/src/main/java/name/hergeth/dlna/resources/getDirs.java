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

import name.hergeth.dlna.api.StringPair;
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
    public StringPair getDirs(@QueryParam("name") String name, @QueryParam("from") Optional<String> f) {
    	try{
	        final String from = f.or("0");
	
	    	Device dev = dlnac.findDeviceUID(name);
	    	
	    	if( dev == null ){
	    		String[][] sa = new String[1][1];
	    		sa[0][0] = new String("device "+name+" not found.");
	    		return  new StringPair(counter.incrementAndGet(), sa);
	    	}
	    	
	    	DirContent dc = dlnac.getDirContent(dev, from);
	    	if( dc.getDirsSize() == 0){
	    		String[][] sa = new String[1][1];
	    		sa[0][0] = new String("no more directories in "+from +".");
	    		return  new StringPair(counter.incrementAndGet(), sa);
	    	}
	    	
	    	Container[] dirs = dc.getDirs();
	    	String[][] dm = new String[dirs.length][2];
	    	int i = 0;
	    	for(Container c : dirs ){
	    		dm[i][0] = c.getId();
	    		dm[i++][1] = c.getTitle();
	    	}
	        return new StringPair(counter.incrementAndGet(), dm);
    	}catch(Exception e){
    		throw new WebApplicationException(e.getMessage());
    	}
    }
}