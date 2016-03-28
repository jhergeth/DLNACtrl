package name.hergeth.dlna.resources;

import java.util.concurrent.atomic.AtomicLong;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.fourthline.cling.model.meta.Device;
import org.fourthline.cling.support.model.container.Container;

import com.codahale.metrics.annotation.Timed;

import name.hergeth.dlna.DLNAApplication;
import name.hergeth.dlna.api.OneString;
import name.hergeth.dlna.api.StringArray;
import name.hergeth.dlna.api.StringPair;
import name.hergeth.dlna.core.DLNACtrl;

public class getDevice {
	private final DLNACtrl dlnac;
    private final AtomicLong counter;
    private final String type;

    public getDevice(DLNACtrl c, String type) {
    	dlnac = c;
    	this.type = type;
        this.counter = new AtomicLong();
    }

    public StringPair getDevices() {
    	Device[] dc = dlnac.getDevices(type);
    	if( dc == null || dc.length == 0){
    		String[][] sa = new String[1][1];
    		sa[0][0] = new String("no devices of type: "+type+" found.");
    		return  new StringPair(counter.incrementAndGet(), sa);
    	}
    	
    	String[][] dm = new String[dc.length][2];
    	int i = 0;
    	for(Device c : dc ){
    		dm[i][0] = c.getIdentity().getUdn().getIdentifierString();
    		dm[i++][1] = c.getDetails().getFriendlyName();
    	}

        return new StringPair(counter.incrementAndGet(), dm);
    }
}