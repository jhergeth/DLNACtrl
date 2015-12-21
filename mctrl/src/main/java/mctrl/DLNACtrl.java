package mctrl;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.logging.Level;

import org.fourthline.cling.UpnpService;
import org.fourthline.cling.UpnpServiceImpl;
import org.fourthline.cling.controlpoint.ActionCallback;
import org.fourthline.cling.controlpoint.ControlPoint;
import org.fourthline.cling.controlpoint.SubscriptionCallback;
import org.fourthline.cling.model.action.ActionArgumentValue;
import org.fourthline.cling.model.action.ActionInvocation;
import org.fourthline.cling.model.gena.CancelReason;
import org.fourthline.cling.model.gena.GENASubscription;
import org.fourthline.cling.model.message.UpnpResponse;
import org.fourthline.cling.model.message.header.STAllHeader;
import org.fourthline.cling.model.message.header.UDAServiceTypeHeader;
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
import org.fourthline.cling.model.types.UDAServiceType;
import org.fourthline.cling.model.types.UDN;
import org.fourthline.cling.registry.Registry;
import org.fourthline.cling.registry.RegistryListener;
import org.fourthline.cling.support.avtransport.callback.Play;
import org.fourthline.cling.support.avtransport.callback.SetAVTransportURI;
import org.fourthline.cling.support.avtransport.lastchange.AVTransportLastChangeParser;
import org.fourthline.cling.support.avtransport.lastchange.AVTransportVariable;
import org.fourthline.cling.support.contentdirectory.callback.Browse;
import org.fourthline.cling.support.lastchange.LastChange;
import org.fourthline.cling.support.model.BrowseFlag;
import org.fourthline.cling.support.model.DIDLContent;
import org.fourthline.cling.support.model.DIDLObject;
import org.fourthline.cling.support.model.SortCriterion;
import org.fourthline.cling.support.model.TransportState;
import org.fourthline.cling.support.model.container.Container;
import org.fourthline.cling.support.model.item.ImageItem;
import org.fourthline.cling.support.model.item.Item;
import org.fourthline.cling.support.model.item.VideoItem;

public class DLNACtrl {
	final public static String version = "0.2.8";

	
	final long PICTIME = 30*1000;


	public PlayJob currentJob;
	public PlayJob newJob = null;

	UpnpService upnpService = null;
	Registry registry = null; 
	UDAServiceType typeContent = null;
	UDAServiceType typeRenderer = null;

	public DLNACtrl(){
	}

	/**
	 * @param args
	 * @throws Exception
	 */
	public void runIt(String[] args) throws Exception {

		// UPnP discovery is asynchronous, we need a callback
		RegistryListener listener = new DLNAListener(this);


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

		newJob = null;
		currentJob = new PlayJob();
	}

	public void shutDown() {
		// Release all resources and advertise BYEBYE to other UPnP devices
		Main.jlog.log(Level.INFO, "Stopping Cling...");
		upnpService.shutdown();
	}

	public long timeToLong(String s){
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
	 * Functions to be used from JavaScript
	 */

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
	public Device[] getDevices(final String t){
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
	 * getDevice(String dev, String type)
	 * Get Device of type type with name dev
	 */
	public Device getDevice(final String dev, final String typ){
		Device[] devs = getDevices(typ);
		for( Device d : devs){
			if(d.getDetails().getFriendlyName().equalsIgnoreCase(dev))
				return d;
		}
		return null;
	}

	/**
	 * getDeviceNames( String t )
	 * Gets the names of all devices of a specific service type (t).
	 * @param t		Service type of the devices
	 * @return String[]	array of Devicenames 
	 */
	public String[] getDeviceNames(final String t){
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
	public String[] getServerArray(){
		return getDeviceNames("ContentDirectory");
	}

	/**
	 * getRendererArray()
	 * Returns an array of the names of all known renderer.
	 * @return String[]
	 */
	public String[] getRendererArray(){
		return getDeviceNames("AVTransport");
	}


	public String[] devBrowseItem(final String devName, final String from ){
		Device server = registry.getDevice(new UDN(devName), false) ;
		DirContent dirc = getDirContent(server, from);
		String[] s = new String[1];

		return dirc.items.toArray(s);
	}


	public String[] devBrowseDir(final String devName, final String from ){
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

	public DirContent browseTo(Service s, final String from){
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

	private DirContent browseTo(Service s, final String from, final String to){
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

	private void doBrowse(Service s, String from,
			BiConsumer<ActionInvocation, DIDLContent> func) {

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


	public int getDirSize(PlayJob job){
		if(checkJob(job))
			return getDirSize(getServerDev(job), job.getPlaylist());
		return 0;
	}

	public int getDirSize(Service theSource, final String from){
		final AtomicReference<Integer> res = new AtomicReference<Integer>();

		doBrowse(theSource, from, (ActionInvocation actionInvocation, DIDLContent didl) -> {
			List<Item> items = didl.getItems();
			res.set(items.size());
		});

		return res.get();
	}


	public Service findRenderer(String sDevice) {
		return findService(sDevice, typeRenderer);
	}

	public Service findScreen(String sDevice) {
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


	public Service getScreenDev(PlayJob job) {
		Collection<Device> devs = registry.getDevices(typeRenderer);
		for( Device dev : devs){
			Main.jlog.log(Level.INFO, "Checking " + dev.getDetails().getFriendlyName() + " to be " + job.getScreen());
			if( dev.getDetails().getFriendlyName().equalsIgnoreCase(job.getScreen())){
				return dev.findService(typeRenderer);

			}
		}
		return null;
	}

	public Service getServerDev(PlayJob job) {
		Collection<Device> devs = registry.getDevices(typeContent);
		for( Device dev : devs){
			Main.jlog.log(Level.INFO, "Checking " + dev.getDetails().getFriendlyName() + " to be " + job.getServer());
			if( dev.getDetails().getFriendlyName().equalsIgnoreCase(job.getServer())){
				return dev.findService(typeContent);

			}
		}
		return null;
	}

	
	public boolean checkJob(PlayJob job){
		Service	theSource = findService(job.getServer(), typeContent);
		if(theSource == null ){
			Main.jlog.log(Level.WARNING, "No source defined! {" + job.getServer() +"}");
			return false;
		}
		Service theScreen = findService(job.getScreen(), typeRenderer);
		if(theScreen == null ){
			Main.jlog.log(Level.WARNING, "No renderer defined! {" + job.getScreen() +"}");
			return false;
		}
		Item item = job.getItemFromServer(theSource, upnpService.getControlPoint());
		if(item == null ){
			Main.jlog.log(Level.WARNING, "Empty playlist! {" + job.getPlaylist() +"}");
			return false;
		}
		
		return true;			
	}
	
	

	public void jumpForward(){
		if(checkJob(currentJob) && !currentJob.getStatus().equalsIgnoreCase("idle")){
			currentJob.jumpForward();
		}
	}

	public void jumpBack(){
		if(checkJob(currentJob) && !currentJob.getStatus().equalsIgnoreCase("idle")){
			currentJob.jumpBack();
		}
	}

	public void stop(){
		if(checkJob(currentJob) && !currentJob.getStatus().equalsIgnoreCase("idle")){
			currentJob.setRest(Integer.MAX_VALUE);
		}
	}

	public void play(){
		if(checkJob(currentJob) && !currentJob.getStatus().equalsIgnoreCase("idle")){
			currentJob.setRest(currentJob.getTotal());
		}
	}

	public PlayJob getStatus(){
		return currentJob;
	}

	/**
	 * Play job/playlist
	 * @param sRenderer
	 * @param sDir
	 * @param theId
	 * @param duration
	 */
	public void play(String sRenderer, String sDir, String theId, int duration){
		play( new PlayJob("starting", theId, sRenderer, sDir, 0, duration, duration, duration ), duration);
	}

	public void play(PlayJob job,  int duration){
		ControlPoint ctrlPoint = upnpService.getControlPoint();

		Main.jlog.log(Level.INFO, "Play " + job.getPlaylist() + " from " + job.getServer() + " to " + job.getScreen());

		if(!checkJob(job)){
			return;
		}

		if(duration < 5000 ){
			Main.jlog.log(Level.WARNING, "Duration below 5 seconds! {" + duration +"}");
			return;
		}

		while(currentJob.hasStatus("idle")){
			currentJob = job;
			Main.jlog.log(Level.INFO, "Starting rendering of playlist " + currentJob.getPlaylist());
			
			for( int m = 0; m < getDirSize(job); m++){				// for all items in playlist
				currentJob.setItem(m);
				
				Service theScreen = getScreenDev(currentJob);		// find a renderer
				if(theScreen == null){
					Main.jlog.log(Level.WARNING, "No rendering service found!");
					currentJob.setStatus("renderer mising");

					int cnt = 0;
					do{
						if(cnt%12 == 0){
						// Broadcast a search message for all devices
							ctrlPoint.search(new STAllHeader());
						}
						try {
							Main.jlog.log(Level.INFO, "Waiting 5 seconds for renderer...");
							Thread.sleep(5000);
						} catch (InterruptedException e) {
						}
						theScreen = getScreenDev(currentJob);
						cnt++;
					}while(theScreen == null);

				}
				Main.jlog.log(Level.INFO, "Rendering service found! Checking media server...");

				currentJob.setStatus("searching media server");
				
				Service theSource = getServerDev(currentJob);
				if(theSource == null){								// no media server!!!
					Main.jlog.log(Level.WARNING, "No media directory found!");
					currentJob.setStatus("media directory mising");
				}
				else{
					Main.jlog.log(Level.INFO, "Media directory found! Checking item ...");
					
					Item item = currentJob.getItemFromServer( theSource, ctrlPoint);
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
						} catch (InterruptedException | ExecutionException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
							currentJob.setStatus("exception during play");
						}
						
						if(newJob != null){
							currentJob = newJob;
							newJob = null;
						}
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
			currentJob = new PlayJob();
		}
		if(!currentJob.hasStatus("idle"))
		{
			Main.jlog.log(Level.INFO, "Rendering is already running, just changed playlist.");
			newJob = job;
			newJob.setItem(0);
			currentJob.setRest(0);
		}
	}

	public void playItemOnScreen(Service theScreen, Item item, int duration)
			throws InterruptedException, ExecutionException {

		MySubscriptionCallback callback;
		callback = new MySubscriptionCallback(theScreen, 10);
		upnpService.getControlPoint().execute(callback);

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
			switch (callback.theState) {
			case PLAYING :
				Main.jlog.log(Level.INFO, "State is: playing " + currentJob.getRest() + " seconds media=" + uri);
				break;
			case STOPPED :
				Main.jlog.log(Level.INFO, "State is: stopped " + currentJob.getRest() + " seconds media=" + uri);
				break;
			case CUSTOM :
				Main.jlog.log(Level.INFO, "State is: custom " + currentJob.getRest() + " seconds media=" + uri);
				break;
			case NO_MEDIA_PRESENT :
				Main.jlog.log(Level.INFO, "State is: no media present " + currentJob.getRest() + " seconds media=" + uri);
				break;
			case PAUSED_PLAYBACK :
				Main.jlog.log(Level.INFO, "State is: paused playback " + currentJob.getRest() + " seconds media=" + uri);
				break;
			case PAUSED_RECORDING :
				Main.jlog.log(Level.INFO, "State is: paused recording " + currentJob.getRest() + " seconds media=" + uri);
				break;
			case RECORDING :
				Main.jlog.log(Level.INFO, "State is: recording " + currentJob.getRest() + " seconds media=" + uri);
				break;
			default:
//				Main.jlog.log(Level.INFO, "State is: DEFAULT " + currentJob.getRest() + " seconds media=" + uri);
				break;
			}
			currentJob.setStatus("playing");
			time = currentJob.getRest();
		}
		currentJob.setStatus("medium finished");
		currentJob.setRest(0);
	}

	private String sendURIandPlay(Service theScreen, Item item) throws InterruptedException, ExecutionException {
		String uri = item.getFirstResource().getValue();
		Future<String> fa = null;

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
		fa = upnpService.getControlPoint().execute(setAVTransportURIAction);

		fa.get();

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
		fa = upnpService.getControlPoint().execute(playAction);
		fa.get();
		return uri;
	}

	public void renderPlaylist()
			throws InterruptedException, ExecutionException {
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
