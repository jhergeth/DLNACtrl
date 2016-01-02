package mctrl;

import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.logging.Level;

import org.fourthline.cling.UpnpService;
import org.fourthline.cling.UpnpServiceImpl;
import org.fourthline.cling.controlpoint.ActionCallback;
import org.fourthline.cling.controlpoint.ControlPoint;
import org.fourthline.cling.controlpoint.SubscriptionCallback;
import org.fourthline.cling.model.action.ActionInvocation;
import org.fourthline.cling.model.message.UpnpResponse;
import org.fourthline.cling.model.message.header.STAllHeader;
import org.fourthline.cling.model.message.header.UDAServiceTypeHeader;
import org.fourthline.cling.model.meta.Device;
import org.fourthline.cling.model.meta.Service;
import org.fourthline.cling.model.types.UDAServiceType;
import org.fourthline.cling.model.types.UDN;
import org.fourthline.cling.registry.Registry;
import org.fourthline.cling.registry.RegistryListener;
import org.fourthline.cling.support.avtransport.callback.Play;
import org.fourthline.cling.support.avtransport.callback.SetAVTransportURI;
import org.fourthline.cling.support.contentdirectory.callback.Browse;
import org.fourthline.cling.support.contentdirectory.callback.Browse.Status;
import org.fourthline.cling.support.model.BrowseFlag;
import org.fourthline.cling.support.model.DIDLContent;
import org.fourthline.cling.support.model.DescMeta;
import org.fourthline.cling.support.model.Res;
import org.fourthline.cling.support.model.SortCriterion;
import org.fourthline.cling.support.model.container.Container;
import org.fourthline.cling.support.model.item.Item;

public class DLNACtrl {

	public static final String version = "0.2.10";
	public static UpnpService upnpService = null;
	public static ControlPoint ctrlPoint = null;

	final long PICTIME = 100*1000;
	public DLNACtrlPlaySimple pCtrl = null;
	Registry registry = null;
	UDAServiceType typeContent = null;
	UDAServiceType typeRenderer = null;
	
	ExecutorService execPlay;
	
	private Future<?> waitForPlay = null;

	public DLNACtrl() {
		super();
		pCtrl = new DLNACtrlPlaySimple(this);
	}

	/**
	 * Static methods
	 * @return
	 */
	public static <T> Future<T> execAction( ActionCallback cb){
		 return ctrlPoint.execute(cb);
}

	public static void execAction( SubscriptionCallback cb){
		ctrlPoint.execute(cb);
}

	/**
	 * @param args
	 * @throws Exception
	 */
	public void runIt(String[] args) throws Exception {

		// UPnP discovery is asynchronous, we need a callback
		RegistryListener listener = new DLNAListener(this);
		Main.jlog.setLevel( Level.INFO );

		// This will create necessary network resources for UPnP right away
		Main.jlog.log(Level.INFO, "Starting Cling...version: " + version);
		upnpService = new UpnpServiceImpl(listener);
		ctrlPoint = upnpService.getControlPoint();
		registry = upnpService.getRegistry();

		Main.jlog.log(Level.INFO, "Waiting 1 second...");


		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
		}
		// Send a search message to all devices, search for MediaServer, they should respond soon
		typeRenderer = new UDAServiceType("AVTransport");
		typeContent = new UDAServiceType("ContentDirectory");
		searchUDAServiceType(null);

		execPlay = Executors.newFixedThreadPool(1);
	}

	public Registry getRegistry(){
		return registry;
	}
	
	public PlayJob getJob(){
		return pCtrl.getJob();
	}
	
	public void shutDown() {
		// Release all resources and advertise BYEBYE to other UPnP devices
		Main.jlog.log(Level.INFO, "Stopping Cling...");
		
    	// Shutdown executors
    	try {
    		Main.jlog.log(Level.INFO, "attempt to shutdown executor");
    	    execPlay.shutdown();
    	    execPlay.awaitTermination(11, TimeUnit.SECONDS);
    	}
    	catch (InterruptedException e) {
    		Main.jlog.log(Level.SEVERE, "tasks interrupted");
    	}
    	finally {
    	    if (!execPlay.isTerminated()) {
    	    	Main.jlog.log(Level.SEVERE, "cancel non-finished tasks");
    	    }
    	    execPlay.shutdownNow();
    	    Main.jlog.log(Level.INFO, "shutdown finished");
    	}


		upnpService.shutdown();
	}


	public void sendSearch() {
		ctrlPoint.search(new STAllHeader());
	}
	
	public void restart(String devName){
		if(waitForPlay != null && waitForPlay.isDone()){
			startPlayThread();
		}
	}

	/**
	 * search UDAServiceType
	 * @param t	String	send a search message for service type
	 * 					if t is null all service types are searched
	 */
	public void searchUDAServiceType(final String t) {
		// Send a search message to all devices, search for MediaServer, they should respond soon
		if(t == null ){
			sendSearch();
		}
		else{
			UDAServiceType type = new UDAServiceType(t);
			ctrlPoint.search(new UDAServiceTypeHeader(type));
		}
	}

	/**
	 * waitForRenderer
	 * Waits until a renderer has been registered in the UPNP-Registry
	 */
	public void waitForRenderer() {
		Collection<Device> renderer = null;
		do{
			renderer = registry.getDevices(typeRenderer);
			if(renderer.isEmpty()){
				// Broadcast a search message for all devices
				sendSearch();
				try {
					Main.jlog.log(Level.INFO, "Waiting 5 seconds for renderer...");
					Thread.sleep(5000);
				} catch (InterruptedException e) {
				}
			}
		}while(renderer.isEmpty());
	}

	/**
	 * getDevices( String t )
	 * Gets array of all devices with a specific service type
	 * @param t		UDAServiceType of devices 
	 * @return	array of Devices
	 */
	public Device[] getDevices(final String t) {
		if( upnpService != null ){
			Collection<Device> devs = registry.getDevices(new UDAServiceType(t));
			Main.jlog.log(Level.INFO, "getDevices: found " + devs.size() + " devices.");

			if(devs.size() > 0){
				Device[] d = new Device[1];
				return devs.toArray(d);
			}
			else{
				return new Device[0];
			}
		}
		else{
			Main.jlog.log(Level.SEVERE, "ERROR: upnpService not initialized!");
		}
		return null;
	}

	/**
	 * getDeviceNames( String t )
	 * Gets the names of all devices of a specific service type (t).
	 * @param t		Service type of the devices
	 * @return String[]	array of Devicenames 
	 */
	public String[] getDeviceNames(final String t) {
		Device[] devs = getDevices(t);
		String[] res = new String[1];

		if( devs != null ){
			if(devs.length > 0 ){
				Main.jlog.log(Level.INFO, "GetDeviceNames: found " + devs.length + " devices.");
				res = new String[devs.length];
				for( int i = 0; i < devs.length; i++){
					res[i] = devs[i].getDetails().getFriendlyName();
				}
			}
			else{
				res[0] = "No devices found.";
			}
		}
		else{
			res[0] = "ERROR: upnpService not initialized!";
		}
		return res;
	}

	/**
	 * getServerArray
	 * Returns a String array of all found UPNP Server names
	 * @return String[]
	 */
	public String[] getServerArray() {
		return getDeviceNames("ContentDirectory");
	}

	/**
	 * getRendererArray()
	 * Returns an array of the names of all known renderer.
	 * @return String[]
	 */
	public String[] getRendererArray() {
		return getDeviceNames("AVTransport");
	}

	public Service findRenderer(String sDevice) {
		return findService(sDevice, typeRenderer);
	}

	public Service findVault(String sDevice) {
		return findService(sDevice, typeContent);
	}

	public Service findService(String sDevice, UDAServiceType type) {
		Collection<Device> devs = registry.getDevices(type);
		for( Device dev : devs){
			Main.jlog.log(Level.INFO, "Checking " + dev.getDetails().getFriendlyName() + " to be " + sDevice);
			if( dev.getDetails().getFriendlyName().equalsIgnoreCase(sDevice)){
				return dev.findService(type);

			}
		}
		Main.jlog.log(Level.WARNING, "Did NOT find " + sDevice);
		return null;
	}

	public String[] devBrowseItem(final String devName, final String from) {
		Device server = registry.getDevice(new UDN(devName), false) ;
		DirContent dirc = getDirContent(server, from);
		String[] s = new String[1];

		return dirc.items.toArray(s);
	}

	public String[] devBrowseDir(final String devName, final String from) {
		Device server = registry.getDevice(new UDN(devName), false) ;
		DirContent dirc = getDirContent(server, from);
		String[] s = new String[1];

		return dirc.dirs.toArray(s);
	}

	public DirContent getDirContent(final Device server, final String from) {
		Service srv = server.findService(typeContent);

		if( srv != null ){
			DirContent dirc = browseTo(srv, from);  		
			return dirc;
		}
		else{
			Main.jlog.log(Level.WARNING, "ERR: Server " + server.getDetails().getFriendlyName() + " is no media server!");
			return null;
		}
	}

	public DirContent browseTo(Service s, final String from) {
		final AtomicReference<DirContent> res = new AtomicReference<DirContent>();

		doBrowse(s, from, (ActionInvocation actionInvocation, DIDLContent didl) -> {
			if( didl != null){
				Main.jlog.log(Level.INFO, "browseTo: from="+ from + "\n" + printDIDL(didl) + "\n" );
				res.set(new DirContent(from, from, didl.getItems(), didl.getContainers()));
			}
			else
				res.set(null);
			return;
		});

		DirContent ret = res.get();
		if(ret != null)
			Main.jlog.log(Level.INFO, "browseTo: res="+ ret.id);
		return ret;

	}

	private DirContent browseTo(Service s, final String from, final String to) {
		final AtomicReference<DirContent> res = new AtomicReference<DirContent>();

		doBrowse(s, from, (ActionInvocation actionInvocation, DIDLContent didl) -> {
			for( Container cont : didl.getContainers()){
				if( cont.getTitle().equalsIgnoreCase(to)){
					Main.jlog.log(Level.INFO, "browseTo: from="+ from + " to=" + to + " Container: " + cont.getTitle() + "\n" + printDIDL(didl));
					res.set(new DirContent(cont.getId(), cont.getTitle(), cont.getItems(), cont.getContainers()));
					return;
				}
			}
		});

		DirContent ret = res.get();
		if(ret != null)
			Main.jlog.log(Level.INFO, "browseTo: res="+ ret.id);
		return ret;

	}
	
	private String printDIDL(DIDLContent d){
		String res = "";
		res += d.getContainers().size() + " containers:\n";
		for( Container cont : d.getContainers()){
			res += cont.getTitle() + "["+cont.getFirstResource().getValue() +"]" +  ", ";
		}
		res += "\n";
		
		res += d.getItems().size() + " items ";
		for( Item item : d.getItems()){
			res += item.getTitle() + ", ";
		}
		res += "\n";
		
		res += d.getDescMetadata().size() + " metadata ";
		for( DescMeta m : d.getDescMetadata()){
			res += m.getId() + ", ";
		}
		res += "\n";
		
		
		return res;
	}

	public int getDirSize(PlayJob job) {
		if(job != null && job.checkJob())
			return getDirSize(findVault(job.getServer()), job.getPlaylist());
		return 0;
	}

	public int getDirSize(Service theSource, final String from) {
		if(theSource == null ){
			Main.jlog.log(Level.WARNING, "getDirSize: no source defined!");
			return 0;
		}
		if(from == null || from.length() == 0){
			Main.jlog.log(Level.WARNING, "getDirSize: no from defined!");
			return 0;
		}

		final AtomicReference<Integer> res = new AtomicReference<Integer>();

		doBrowse(theSource, from, (ActionInvocation actionInvocation, DIDLContent didl) -> {
			List<Item> items = didl.getItems();
			res.set(items.size());
		});

		return res.get();
	}

	protected void doBrowse(Service s, String from, BiConsumer<ActionInvocation, DIDLContent> func) {

		ActionCallback doBrowseAction  = new Browse(s, from, BrowseFlag.DIRECT_CHILDREN) {
			@Override
			public void received(ActionInvocation actionInvocation, DIDLContent didl) {
				func.accept(actionInvocation, didl);
			}

			@Override
			public void updateStatus(Status status) {
				Main.jlog.log(Level.INFO, "doBrowse: " + s.toString() + " browse status: " + status );
			}

			@Override
			public void failure(ActionInvocation invocation,
					UpnpResponse operation,
					String defaultMsg) {
				// Something wasn't right...
				Main.jlog.log(Level.WARNING, "doBrowse: " + s.toString() + " failure: " + defaultMsg );
				DIDLContent didl = new DIDLContent();
				func.accept(actionInvocation, didl);
			}
		};
		Future<String> fu = upnpService.getControlPoint().execute(doBrowseAction);

		try {
			fu.get();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return;
	}

	public Item getItemFromServer(Service server, String playlist, int item) {
		if( server == null){
			Main.jlog.log(Level.WARNING, "getItemFromServer: No server!");
			return null;
		}
		
		//		Service theSource, final String from, final int no){
		final AtomicReference<Item> res = new AtomicReference<Item>();

		ActionCallback doBrowseAction  = new Browse(server, playlist, BrowseFlag.DIRECT_CHILDREN, 
				"*", item, new Long(1), (SortCriterion[])null) {

			@Override
			public void received(ActionInvocation actionInvocation, DIDLContent didl) {
				List<Item> items = didl.getItems();
				if( items.size() > 0 ){
					res.set(items.get(0));
				}
				else{
					res.set(null);
				}
			}

			@Override
			public void updateStatus(Status status) {
			}

			@Override
			public void failure(ActionInvocation invocation,
					UpnpResponse operation,
					String defaultMsg) {
				// Something wasn't right...
				Main.jlog.log(Level.WARNING, "getItemFromServer: " + defaultMsg + " \nwhile:\nserver="+server.getDevice().getDisplayString() + "\nplaylist=" + playlist + "\nitem="+item );
				res.set(null);
			}
		};
		Future<String> f = ctrlPoint.execute(doBrowseAction);

		try {
			f.get();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return res.get();
	}

	public void jumpForward() {
		pCtrl.jumpForward();
	}

	public void jumpBack() {
		pCtrl.jumpBack();
	}

	public void stop() {
		pCtrl.stop();
	}

	public void play() {
		pCtrl.play();
	}

//	public PlayJob getStatus() {
//		return currentJob;
//	}

	/**
	 * Play job/playlist
	 * @param sRenderer
	 * @param sDir
	 * @param theId
	 * @param duration
	 */
	public void play(String sRenderer, String sDir, String theId, int duration) {
		play( new PlayJob("starting", theId, sRenderer, sDir, 0, duration, duration, duration ), duration);
	}

	public void play(PlayJob job, int duration) {
		Main.jlog.log(Level.INFO, "Play " + job.getPlaylist() + " from " + job.getServer() + " to " + job.getScreen());

		if(!job.checkJob()){
			return;
		}

		if(duration < 5000 ){
			Main.jlog.log(Level.WARNING, "Duration below 5 seconds! {" + duration +"}");
			return;
		}

		if(waitForPlay == null || waitForPlay.isDone()){
			pCtrl.setJob(job);
			
	    	startPlayThread();
		}
		else{
			Main.jlog.log(Level.INFO, "Rendering is already running, attempting to change playlist.");
			pCtrl.setJob(job);
		}
	}

	public void startPlayThread() {
		waitForPlay = execPlay.submit(() -> {
			Main.jlog.log(Level.INFO, "Starting play job...");
			while(true){
				pCtrl.doPlay();
				if(!pCtrl.isRunning()){
					Main.jlog.log(Level.INFO, "Play aborted due to 'stop' status.");
					return;
				}
				Main.jlog.log(Level.INFO, "Play rollover....");
			}
		});
	}



}