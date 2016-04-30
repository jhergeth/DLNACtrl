package name.hergeth.dlna.core;

import org.fourthline.cling.model.meta.LocalDevice;
import org.fourthline.cling.model.meta.RemoteDevice;
import org.fourthline.cling.registry.Registry;
import org.fourthline.cling.registry.RegistryListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DLNAListener implements RegistryListener {
	private Logger jlog = LoggerFactory.getLogger("name.hergeth.dlna.core");

	DLNACtrl theCtrl;

	public DLNAListener(DLNACtrl c) {
		theCtrl = c;
	}

	public void remoteDeviceDiscoveryStarted(Registry registry, RemoteDevice device) {
		jlog.info("Discovery started: " + device.getDisplayString());
	}

	public void remoteDeviceDiscoveryFailed(Registry registry, RemoteDevice device, Exception ex) {
		jlog.warn("Discovery failed: " + device.getDisplayString() + " => " + ex);
	}

	public void remoteDeviceAdded(Registry registry, RemoteDevice device) {
		jlog.info("Device added: " + device.getDisplayString());
		theCtrl.restart(device.getDetails().getFriendlyName());
		/*
		 * RemoteService[] srv = device.findServices(); for( RemoteService ss :
		 * srv ){ jLog.log(Level.INFO, "Found service: " + ss.toString() + " @ "
		 * + device.getDisplayString()); Action<RemoteService>[] actions =
		 * ss.getActions(); for( Action<RemoteService> a : actions){
		 * jLog.log(Level.INFO, "Found action: " + a.getName() + " @ " +
		 * device.getDisplayString()); } }
		 */
	}

	public void remoteDeviceUpdated(Registry registry, RemoteDevice device) {
		// jlog.info("Device updated: " +
		// device.getDisplayString());
		/*
		 * devices.add(device, "Remote device updated: " +
		 * device.getDetails().getFriendlyName() + "["+
		 * device.getDisplayString() + " | " + device.toString() + "]" );
		 */ }

	public void remoteDeviceRemoved(Registry registry, RemoteDevice device) {
		jlog.info("Remote device removed: " + device.getDisplayString());
	}

	public void localDeviceAdded(Registry registry, LocalDevice device) {
		jlog.info("Local device added: " + device.getDisplayString());
	}

	public void localDeviceRemoved(Registry registry, LocalDevice device) {
		jlog.info("Local device removed: " + device.getDisplayString());
	}

	public void beforeShutdown(Registry registry) {
		/*
		 * jLog.log(Level.INFO, "Before shutdown, the registry has devices: " +
		 * registry.getDevices().size() );
		 */ }

	public void afterShutdown() {
		jlog.info("Shutdown of registry complete!");

	}
};
