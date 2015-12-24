package mctrl;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;

import org.fourthline.cling.controlpoint.ActionCallback;
import org.fourthline.cling.controlpoint.SubscriptionCallback;
import org.fourthline.cling.model.action.ActionArgumentValue;
import org.fourthline.cling.model.action.ActionInvocation;
import org.fourthline.cling.model.gena.CancelReason;
import org.fourthline.cling.model.gena.GENASubscription;
import org.fourthline.cling.model.message.UpnpResponse;
import org.fourthline.cling.model.meta.Action;
import org.fourthline.cling.model.meta.ActionArgument;
import org.fourthline.cling.model.meta.Device;
import org.fourthline.cling.model.meta.DeviceDetails;
import org.fourthline.cling.model.meta.RemoteDevice;
import org.fourthline.cling.model.meta.RemoteDeviceIdentity;
import org.fourthline.cling.model.meta.RemoteService;
import org.fourthline.cling.model.meta.Service;
import org.fourthline.cling.model.state.StateVariableValue;
import org.fourthline.cling.model.types.UDAServiceId;
import org.fourthline.cling.support.avtransport.lastchange.AVTransportLastChangeParser;
import org.fourthline.cling.support.avtransport.lastchange.AVTransportVariable;
import org.fourthline.cling.support.lastchange.LastChange;
import org.fourthline.cling.support.model.DIDLContent;
import org.fourthline.cling.support.model.DIDLObject;
import org.fourthline.cling.support.model.TransportState;
import org.fourthline.cling.support.model.container.Container;
import org.fourthline.cling.support.model.item.ImageItem;
import org.fourthline.cling.support.model.item.Item;
import org.fourthline.cling.support.model.item.VideoItem;

public class DLNAPrinter extends DLNACtrl {

	public DLNAPrinter(){
	}

	public void renderPlaylist() throws InterruptedException, ExecutionException {
		Service theScreen = null;
		Device theServer = null;
		Service theSource = null;
		//		MySubscriptionCallback callback = null;
		DirContent theDirectory = null;
	
		theScreen = findService("LTDN42K680XWSEU3DA0CB", typeRenderer);
	
		Collection<Device> server = registry.getDevices(typeContent);
		for( Device dev : server){
			Main.jlog.log(Level.INFO, "Found Server " + dev.getDisplayString() + " | " + dev.getDetails().getFriendlyName());
			//      	printDevice(dev);
			//      	printDirectory(dev, "0", 0, "//" + dev.getDetails().getFriendlyName());
	
			theDirectory = getDirContent(dev, "0");
			if(theDirectory != null){
				theDirectory.printDirs();
				theDirectory.printItems();
			}
		}
	
	
		if(theScreen != null && theSource != null && theServer != null && theDirectory != null){
			play("LTDN42K680XWSEU3DA0CB", theSource.getDevice().getDetails().getFriendlyName(), theDirectory.id, (int)PICTIME);
		}
		else{
			if(theScreen == null)
				Main.jlog.log(Level.WARNING, "No renderer found.... ");
	
			if(theSource == null)
				Main.jlog.log(Level.WARNING, "No Media server or directory not found");
		}
	}

	private void printDevice(RemoteDevice dev)
	{
		while(!dev.isFullyHydrated()){
			Main.jlog.log(Level.INFO, "Sleeping for a second...");
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {

			}
		}
		DeviceDetails dd =  dev.getDetails();
		RemoteDeviceIdentity id = dev.getIdentity();
		Main.jlog.log(Level.INFO, "  Identity: " + id.toString());
		Main.jlog.log(Level.INFO, "  URL: " + id.getDescriptorURL());
		Main.jlog.log(Level.INFO, "  MaxAgeSeconds: " + id.getMaxAgeSeconds());
		Main.jlog.log(Level.INFO, "  LocalAdress: " + id.getDiscoveredOnLocalAddress());
		Main.jlog.log(Level.INFO, "  Serial number " + dd.getSerialNumber());
		System.out.print("  MAC: ");
		printByteArray(id.getInterfaceMacAddress());
		Main.jlog.log(Level.INFO, "  UDN: " + id.getUdn());
		System.out.print("  WakeOnLANBytes: ");
		printByteArray(id.getWakeOnLANBytes());
		Main.jlog.log(Level.INFO, "  Device: " + dev.toString());
		DeviceDetails d = dev.getDetails();
		Main.jlog.log(Level.INFO, "  FriendlyName: " + d.getFriendlyName());
		for(RemoteService s : dev.getServices() ){
			Main.jlog.log(Level.INFO, "    RemoteService: " + s.toString());
			Main.jlog.log(Level.INFO, "    ServiceID: " + s.getServiceType());
			Main.jlog.log(Level.INFO, "    Has Actions: " + s.hasActions());
			if(s.hasActions()){
				Action<RemoteService>[] act = s.getActions();
				for( Action<RemoteService> a : act){
					Main.jlog.log(Level.INFO, "      Action: " + a.getName());
					for(ActionArgument<RemoteService> arg : a.getInputArguments()){
						Main.jlog.log(Level.INFO, "        InputArgument: " + arg.getName());
					}
					for(ActionArgument<RemoteService> arg : a.getOutputArguments()){
						Main.jlog.log(Level.INFO, "        OutputArgument: " + arg.getName());
					}
				}
			}
		}

		RemoteService srv = dev.findService(new UDAServiceId("urn:schemas-upnp-org:service:ContentDirectory:1"));
		if( srv != null ){
			Action getBrowseAction = srv.getAction("Browse");
			if( getBrowseAction != null){
				ActionInvocation getBrowseInvocation = new ActionInvocation(getBrowseAction);
				getBrowseInvocation.setInput("ObjectID", 0); // Can throw InvalidValueException
				getBrowseInvocation.setInput("BrowseFlag", "BrowseDirectChildren"); // Can throw InvalidValueException

				ActionCallback getStatusCallback = new ActionCallback(getBrowseInvocation) {

					@Override
					public void success(ActionInvocation invocation) {
						ActionArgumentValue status  = invocation.getOutput("Result");
						if(status != null ){
							Main.jlog.log(Level.INFO, "      R: "+status.getValue());
						}
					}

					@Override
					public void failure(ActionInvocation invocation,
							UpnpResponse operation,
							String defaultMsg) {
						Main.jlog.log(Level.WARNING, defaultMsg);
					}
				};

				upnpService.getControlPoint().execute(getStatusCallback);
			}

		}
	}
	private void printByteArray(byte[] arr){
		if( arr != null ){
			for(byte b : arr){
				System.out.print(Byte.toString(b));
			}
			Main.jlog.log(Level.INFO, "");
		}
		else
			Main.jlog.log(Level.INFO, "<null>");
	}

	private void printItems(DIDLContent didl) {
		int size = didl.getItems().size();
		Main.jlog.log(Level.INFO, "PrintDir:         Number of items: " + size);
		for(int i = 0; i < size; i++){
			Item item = didl.getItems().get(i); 
			if(true || ImageItem.class.equals(item) || VideoItem.class.equals(item)){
				String out = "PrintDir:           [" + item.getId() + "]: Title:" + item.getTitle() +
						" Album:" + item.getFirstPropertyValue(DIDLObject.Property.UPNP.ALBUM.class) +
						" MIMEType: " + item.getFirstResource().getProtocolInfo().getContentFormatMimeType().toString() +
						" URL: " + item.getFirstResource().getValue();
				Main.jlog.log(Level.INFO, out);
			}
		}
	}

	private void printSubDirs( DIDLContent didl, final int dep, final String par, final Service s ) {
		int size = didl.getContainers().size();
		Main.jlog.log(Level.INFO, "PrintDir:         Number of containers: " + size);
		for(int i = 0; i < size; i++){
			Container cont = didl.getContainers().get(i); 
			String out = "PrintDir:           [" + cont.getId() + "]: Title: " + par + "/" +cont.getTitle() +
					" MIMEType: " + cont.getFirstResource().getProtocolInfo().getContentFormatMimeType().toString() +
					" URL: " + cont.getFirstResource().getValue();
			Main.jlog.log(Level.INFO, out);
			if( dep < 3 ){
				printDirectory(s, cont.getId(), dep+1, par + "/" + cont.getTitle() + "/");
			}
		}
	}

	private void printDirectory(final Service s, final String id, final int dep, final String par)
	{
		if(s != null){
			doBrowse(s, id, (ActionInvocation actionInvocation, DIDLContent didl) -> {
				printSubDirs(didl, dep, par, s);
				printItems(didl);
			});
		}
	}
}
