package name.hergeth.dlna.resources;

import java.util.concurrent.atomic.AtomicLong;

import org.fourthline.cling.model.meta.Device;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import name.hergeth.dlna.api.IdName;
import name.hergeth.dlna.core.DLNACtrl;

@SuppressWarnings("rawtypes")
public class getDevice extends ResLogger {
	private final DLNACtrl dlnac;
	private final AtomicLong counter;
	private final String type;

	public getDevice(DLNACtrl c, String type) {
		dlnac = c;
		this.type = type;
		this.counter = new AtomicLong();
	}

	public IdName getDevices() {
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