package mctrl;
import java.util.logging.Level;

import org.fourthline.cling.model.meta.LocalDevice;
import org.fourthline.cling.model.meta.RemoteDevice;
import org.fourthline.cling.registry.Registry;
import org.fourthline.cling.registry.RegistryListener;

public class DLNAListener implements RegistryListener {
	DLNACtrl theCtrl;
	
	public  DLNAListener(DLNACtrl c){
		theCtrl = c;
	}
	
	public void remoteDeviceDiscoveryStarted(Registry registry,
			RemoteDevice device) {
		Main.jlog.log(Level.INFO, "Discovery started: " + device.getDisplayString());
	}

	public void remoteDeviceDiscoveryFailed(Registry registry,
			RemoteDevice device,
			Exception ex) {
		Main.jlog.log(Level.WARNING, 
				"Discovery failed: " + device.getDisplayString() + " => " + ex
				);
	}

	public void remoteDeviceAdded(Registry registry, RemoteDevice device) {
		Main.jlog.log(Level.INFO, "Device added: " + device.getDisplayString());
		if(theCtrl.currentJob != null){
			if(device.getDetails().getFriendlyName().equalsIgnoreCase(theCtrl.currentJob.getScreen())){
				theCtrl.currentJob.setStatus("screen restarted");
			}
		}
		/*            	RemoteService[] srv = device.findServices();
    	for( RemoteService ss : srv ){
			jLog.log(Level.INFO, "Found service: " + ss.toString() + " @ " + device.getDisplayString());
    		Action<RemoteService>[] actions = ss.getActions();
    		for( Action<RemoteService> a : actions){
    			jLog.log(Level.INFO, "Found action: " + a.getName() + " @ " + device.getDisplayString());
    		}
    	}
		 */            }

	public void remoteDeviceUpdated(Registry registry, RemoteDevice device) {
//		Main.jlog.log(Level.INFO, "Device updated: " + device.getDisplayString());
		/*            	devices.add(device, 
    			"Remote device updated: " + device.getDetails().getFriendlyName() + "["+ device.getDisplayString() + " | " + device.toString() + "]"
    		);
		 */            }

	public void remoteDeviceRemoved(Registry registry, RemoteDevice device) {
		Main.jlog.log(Level.INFO, 
				"Remote device removed: " + device.getDisplayString()
				);
	}

	public void localDeviceAdded(Registry registry, LocalDevice device) {
		Main.jlog.log(Level.INFO, 
				"Local device added: " + device.getDisplayString()
				);
	}

	public void localDeviceRemoved(Registry registry, LocalDevice device) {
		Main.jlog.log(Level.INFO, 
				"Local device removed: " + device.getDisplayString()
				);
	}

	public void beforeShutdown(Registry registry) {
		/*                jLog.log(Level.INFO, 
                "Before shutdown, the registry has devices: "
                + registry.getDevices().size()
        );
		 */            }

	public void afterShutdown() {
		Main.jlog.log(Level.INFO, "Shutdown of registry complete!");

	}
};

