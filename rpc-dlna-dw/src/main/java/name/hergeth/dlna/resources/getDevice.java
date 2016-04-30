package name.hergeth.dlna.resources;

import java.util.concurrent.atomic.AtomicLong;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.fourthline.cling.model.meta.Device;
import org.fourthline.cling.support.model.container.Container;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.annotation.Timed;

import name.hergeth.dlna.DLNAApplication;
import name.hergeth.dlna.api.SimpleResult;
import name.hergeth.dlna.api.StringArray;
import name.hergeth.dlna.api.IdName;
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

	public IdName getDevices() {
		Logger jlog = LoggerFactory.getLogger("name.hergeth.dlna.resources");

		Device[] dc = dlnac.getDevices(type);
		if (dc == null || dc.length == 0) {
			String[] id = { "error" };
			String[] n = { "no devices of type: " + type + " found." };
			jlog.info("Did not find devices of type " + type);
			return new IdName(counter.incrementAndGet(), id, n);
		}

		String[] id = new String[dc.length];
		String[] n = new String[dc.length];
		int i = 0;
		for (Device c : dc) {
			id[i] = c.getIdentity().getUdn().getIdentifierString();
			n[i++] = c.getDetails().getFriendlyName();
			jlog.info("Found device:" + id[i - 1] + " name:" + n[i - 1]);
		}

		return new IdName(counter.incrementAndGet(), id, n);
	}
}