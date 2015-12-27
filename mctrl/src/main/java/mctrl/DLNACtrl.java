package mctrl;

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
import org.fourthline.cling.support.model.SortCriterion;
import org.fourthline.cling.support.model.container.Container;
import org.fourthline.cling.support.model.item.Item;

public class DLNACtrl {

	public static final String version = "0.2.10";
	protected static UpnpService upnpService = null;

	final long PICTIME = 30*1000;
	public PlayJob currentJob;
	public PlayJob newJob = null;
	Registry registry = null;
	UDAServiceType typeContent = null;
	UDAServiceType typeRenderer = null;
	
	ExecutorService execPlay;

	public DLNACtrl() {
		super();
	}

	/**
	 * Static methods
	 * @return
	 */
	public static <T> Future<T> execAction( ActionCallback cb){
		 return upnpService.getControlPoint().execute(cb);
}

	public static void execAction( SubscriptionCallback cb){
		 upnpService.getControlPoint().execute(cb);
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
		newJob = null;
		currentJob = new PlayJob();
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

	public long timeToLong(String s) {
		long result = 0;
		String[] parts = s.split("[:.]");
		result += Long.parseLong(parts[0]);
		result *= 60;
		result += Long.parseLong(parts[1]);
		result *= 60;
		result += Long.parseLong(parts[2]);
		result *= 1000;
		result += Long.parseLong(parts[3]);
		return result;
	}

	/**
	 * search UDAServiceType
	 * @param t	String	send a search message for service type
	 * 					if t is null all service types are searched
	 */
	public void searchUDAServiceType(final String t) {
		// Send a search message to all devices, search for MediaServer, they should respond soon
		if(t == null ){
			upnpService.getControlPoint().search(new STAllHeader());
		}
		else{
			UDAServiceType type = new UDAServiceType(t);
			upnpService.getControlPoint().search(new UDAServiceTypeHeader(type));
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
				upnpService.getControlPoint().search(new STAllHeader());
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
			res.set(new DirContent(from, from, didl.getItems(), didl.getContainers()));
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

	public int getDirSize(PlayJob job) {
		if(job.checkJob())
			return getDirSize(findVault(job.getServer()), job.getPlaylist());
		return 0;
	}

	public int getDirSize(Service theSource, final String from) {
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

	public Item getItemFromServer(Service server) {
		//		Service theSource, final String from, final int no){
		final AtomicReference<Item> res = new AtomicReference<Item>();

		ActionCallback doBrowseAction  = new Browse(server, currentJob.getPlaylist(), BrowseFlag.DIRECT_CHILDREN, 
				"*", currentJob.getItem(), new Long(1), (SortCriterion[])null) {

			@Override
			public void received(ActionInvocation actionInvocation, DIDLContent didl) {
				List<Item> items = didl.getItems();
				res.set(items.get(0));
			}

			@Override
			public void updateStatus(Status status) {
			}

			@Override
			public void failure(ActionInvocation invocation,
					UpnpResponse operation,
					String defaultMsg) {
				// Something wasn't right...
			}
		};
		Future<String> f = upnpService.getControlPoint().execute(doBrowseAction);

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
		if(currentJob.checkJob() && !currentJob.getStatus().equalsIgnoreCase("idle")){
			currentJob.jumpForward();
		}
	}

	public void jumpBack() {
		if(currentJob.checkJob() && !currentJob.getStatus().equalsIgnoreCase("idle")){
			currentJob.jumpBack();
		}
	}

	public void stop() {
		if(currentJob.checkJob() && !currentJob.getStatus().equalsIgnoreCase("idle")){
			currentJob.setRest(Integer.MAX_VALUE);
		}
	}

	public void play() {
		if(currentJob.checkJob() && !currentJob.getStatus().equalsIgnoreCase("idle")){
			currentJob.setRest(currentJob.getTotal());
		}
	}

	public PlayJob getStatus() {
		return currentJob;
	}

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

		if(currentJob.hasStatus("idle")){
			currentJob = job;
	    	Future<?> f = execPlay.submit(() -> {
	    		while(!currentJob.hasStatus("stop") ){
	    			doPlay();
					if(currentJob.hasStatus("stop"))
						return;
	    			if(newJob != null){
						Main.jlog.log(Level.INFO, "Playlist " + currentJob.getPlaylist() + " changed to " + newJob.getPlaylist());
						currentJob = newJob;
						newJob = null;
	    			}
	    			else{
	    				currentJob = job;
	    			}
	    		}
				Main.jlog.log(Level.INFO, "Play ended due to 'stop' status.");
	    	});
			Main.jlog.log(Level.INFO, "Play job started.");
		}
		else{
			Main.jlog.log(Level.INFO, "Rendering is already running, attempting to change playlist.");
			newJob = job;
			newJob.setItem(0);
			currentJob.setRest(0);
		}
	}

	private void doPlay() {
		ControlPoint ctrlPoint = upnpService.getControlPoint();

		Main.jlog.log(Level.INFO, "Starting rendering of playlist " + currentJob.getPlaylist());

		for( int m = 0; m < getDirSize(currentJob); m++){				// for all items in playlist
			currentJob.setItem(m);

			Service<?, ?> theScreen = findRenderer(currentJob.getScreen());		// find a renderer
			Service<?, ?> theSource = findVault(currentJob.getServer());;		// find media vault
			
			int cnt = 0;
			while(theScreen == null || theSource == null){
				if( theScreen == null ){
					Main.jlog.log(Level.WARNING, "No rendering service found!");
					currentJob.setStatus("renderer mising");
				}
				else{
					Main.jlog.log(Level.WARNING, "No media directory found!");
					currentJob.setStatus("media directory mising");
				}

				if(cnt%12 == 0){
					// Broadcast a search message for all devices
					ctrlPoint.search(new STAllHeader());
				}
				try {
					Main.jlog.log(Level.INFO, "Waiting 5 seconds for devices...");
					Thread.sleep(5000);
					if(currentJob.hasStatus("stop"))
						return;
					
				} catch (InterruptedException e) {
				}
				theScreen = findRenderer(currentJob.getScreen());		// find a renderer
				theSource = findVault(currentJob.getServer());;		// find media vault
				cnt++;
			}
			Main.jlog.log(Level.INFO, "Rendering service and media diretory found! Checking item...");
			Item item = getItemFromServer( theSource );
			if(item == null){								// no media to render!!
				Main.jlog.log(Level.WARNING, "No item found!");
				currentJob.setStatus("item mising");
			}
			else{
				currentJob.setItemTitle(item.getTitle());
				currentJob.setItemPath(item.getFirstResource().getValue());
				Main.jlog.log(Level.INFO, ".... now rendering "+currentJob.getItemTitle() + " from " + currentJob.getPlaylist() + " to " + currentJob.getScreen());

				try {										// render media on renderer
					currentJob.setStatus("startPlay");
					playItemOnScreen(theScreen, item, currentJob.getPictTime());
					if(currentJob.hasStatus("stop"))
						return;
					
				} catch (InterruptedException | ExecutionException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					currentJob.setStatus("exception during play");
				}

				if(newJob != null){
					return;
				}
			}
			if( currentJob.hasStatus("medium finished")){
				m = currentJob.getItem();
				if(currentJob.back && m > 1){
					m -= 2;
				}
				currentJob.jumpClear();
			}
			else
				m--;
		}

		Main.jlog.log(Level.INFO, "Rendering of playlist " + currentJob.getPlaylist() + " finished..");
	}

	public void playItemOnScreen(Service theScreen, Item item, int duration)
			throws InterruptedException, ExecutionException {
		
		TransportStateCallback.add(theScreen, 10);

		String uri = sendURIandPlay(theScreen, item);
		if(!currentJob.hasStatus("sendPlay")){
			return;
		}

		long time = duration;
		String dur = item.getFirstResource().getDuration();
		if(dur != null){
			Main.jlog.log(Level.INFO, "Duration of "+ uri + " is: " + dur);
			time = timeToLong(dur) + 100;
		}

		if( time < 5000)
			time = 5000;

		currentJob.setStatus("playing");
		currentJob.setTotal((int)(time));
		currentJob.setRest((int)(time));
		Main.jlog.log(Level.INFO, "Sleeping " + time/1000 + " seconds media=" + uri);

		while(time > 0){
			try {
				currentJob.setRest((int)(time-1000));
				Thread.sleep(1000);
			} catch (InterruptedException e) {}

			if(currentJob.hasStatus("screen restarted")){
				Main.jlog.log(Level.INFO, "Resend URI after screen restart " + currentJob.getRest() + " seconds media=" + uri);
				return;
			}
			currentJob.setStatus("playing");
			time = currentJob.getRest();
			Main.jlog.log(Level.INFO, "Waiting " + time + " seconds media=" + uri);
		}
		currentJob.setStatus("medium finished");
		currentJob.setRest(0);
	}

	private String sendURIandPlay(Service theScreen, Item item) throws InterruptedException, ExecutionException {
		String uri = item.getFirstResource().getValue();
		String res = null;

		Main.jlog.log(Level.INFO, "Send media " + uri + " to " + theScreen.getDevice().getDetails().getFriendlyName());

		currentJob.setStatus("sendURL");
		ActionCallback setAVTransportURIAction =
				new SetAVTransportURI(theScreen, uri, "NO METADATA") {
			@Override
			public void failure(ActionInvocation invocation, UpnpResponse operation, String defaultMsg) {
				// Something was wrong
				currentJob.setStatus("failure during SetAVTransport");
				Main.jlog.log(Level.WARNING, "Send media " + uri + " to " + theScreen.getDevice().getDetails().getFriendlyName() + " failed!");
			}
		};
//		fa = upnpService.getControlPoint().execute(setAVTransportURIAction);
		execAction(setAVTransportURIAction).get();

		if(!currentJob.hasStatus("sendURL")){
			return uri;
		}

		Main.jlog.log(Level.INFO, "Playing media " + uri + " on " + theScreen.getDevice().getDetails().getFriendlyName());
		currentJob.setStatus("sendPlay");
		ActionCallback playAction =
				new Play(theScreen) {
			@Override
			public void failure(ActionInvocation invocation, UpnpResponse operation, String defaultMsg) {
				// Something was wrong
				currentJob.setStatus("failure during sendPlay");
				Main.jlog.log(Level.WARNING, "Playing media " + uri + " on " + theScreen.getDevice().getDetails().getFriendlyName() + " failed!");
			}
		};
//		fa = upnpService.getControlPoint().execute(playAction);
		execAction(playAction).get();

		return uri;
	}


}