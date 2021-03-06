package name.hergeth.dlna.core;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;

import org.fourthline.cling.UpnpService;
import org.fourthline.cling.UpnpServiceImpl;
import org.fourthline.cling.controlpoint.ActionCallback;
import org.fourthline.cling.controlpoint.ControlPoint;
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
import org.fourthline.cling.support.model.BrowseFlag;
import org.fourthline.cling.support.model.DIDLContent;
import org.fourthline.cling.support.model.DescMeta;
import org.fourthline.cling.support.model.SortCriterion;
import org.fourthline.cling.support.model.container.Container;
import org.fourthline.cling.support.model.item.Item;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.dropwizard.lifecycle.Managed;
import name.hergeth.dlna.DLNAApplication;

@SuppressWarnings("rawtypes")
public class DLNACtrl implements Managed{

    private Logger jlog = LoggerFactory.getLogger("name.hergeth.dlna.core");
    ExecutorService eSrvc;

	public UpnpService upnpService = null;
	public ControlPoint ctrlPoint = null;
	Registry registry = null;
	UDAServiceType typeContent = null;
	UDAServiceType typeRenderer = null;

	final long PICTIME = 100 * 1000;
//	public DLNACtrlPlaySimple pCtrl = null;
	private Map<String, DLNACtrlPlaySimple> mapPCtrl = new HashMap<>();	
	private Map<String, PlayJob> mapPJobs = new HashMap<>();	
	

	public DLNACtrl() {
		super();
		eSrvc = Executors.newFixedThreadPool(5);
//		pCtrl = new DLNACtrlPlaySimple(this, eSrvc);
	}

	
	@Override
	public void start() throws Exception {
		// UPnP discovery is asynchronous, we need a callback
		RegistryListener listener = new DLNAListener(this);

		// This will create necessary network resources for UPnP right away
		jlog.info("Starting Cling...version: " + getVersion());
		upnpService = new UpnpServiceImpl(listener);
		ctrlPoint = upnpService.getControlPoint();
		registry = upnpService.getRegistry();

		jlog.info("Waiting 1 second...");

		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
		}
		// Send a search message to all devices, search for MediaServer, they
		// should respond soon
		typeRenderer = new UDAServiceType("AVTransport");
		typeContent = new UDAServiceType("ContentDirectory");
		searchUDAServiceType(null);

		String savDir = DLNAApplication.Config().getDefaultSavePath();
		String[] savs = new File(savDir).list( (File d, String f)->{
			return f.endsWith(".json");
		});
		if(savs == null || savs.length == 0)
			jlog.info("No save files found.");
		else{
			for(String f : savs ){
				jlog.info("Found save file: " + savDir + "/" + f);
				PlayJob p = PlayJob.read(savDir+"/"+f);
				DLNACtrlPlaySimple dc = new DLNACtrlPlaySimple(this, eSrvc);
				dc.setJob(p);
				mapPCtrl.put(p.getScreen(), dc);
				dc.startPlayThread(p);
			}
		}
	}

	@Override
	public void stop() throws Exception {
		stopCling();
		
//		pCtrl.end();
		ctrlPoint = null;
		
		for(DLNACtrlPlaySimple p : mapPCtrl.values()){
			p.end();
		}
		mapPCtrl.clear();
	}



	public String getVersion() {
		return getClass().getPackage().getImplementationVersion();
	}

	public ControlPoint getCtrlPoint() {
		return ctrlPoint;
	}

	public Registry getRegistry() {
		return registry;
	}

	public PlayJob getJob(String renderer) {
		DLNACtrlPlaySimple p = mapPCtrl.get(renderer);
		if(p != null)
			return p.getJob();
		else
			return null;
	}

	public PlayJob[] getJobs() {
		PlayJob[] par = new PlayJob[mapPCtrl.size()];
		int i = 0;
		for(DLNACtrlPlaySimple p : mapPCtrl.values()){
			par[i++] = p.getJob();
		}
		return par;
	}

	
	private void stopCling() {
		// Release all resources and advertise BYEBYE to other UPnP devices
		jlog.info("Stopping Cling...");
		upnpService.shutdown();
	}

	public boolean init(){
		stopCling();
		try {
			start();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return upnpService != null && ctrlPoint != null && registry != null;
	}

	public void sendSearch() {
		ctrlPoint.search(new STAllHeader());
	}

	public void restart(String r) {
		DLNACtrlPlaySimple p = mapPCtrl.get(r);
		if(p != null)
			p.startPlayThread();
	}

	/**
	 * search UDAServiceType
	 * 
	 * @param t
	 *            String send a search message for service type if t is null all
	 *            service types are searched
	 */
	public void searchUDAServiceType(final String t) {
		// Send a search message to all devices, search for MediaServer, they
		// should respond soon
		if (t == null) {
			sendSearch();
		} else {
			UDAServiceType type = new UDAServiceType(t);
			ctrlPoint.search(new UDAServiceTypeHeader(type));
		}
	}

	/**
	 * waitForRenderer Waits until a renderer has been registered in the
	 * UPNP-Registry
	 */
	public void waitForRenderer() {
		Collection<Device> renderer = null;
		do {
			renderer = registry.getDevices(typeRenderer);
			if (renderer.isEmpty()) {
				// Broadcast a search message for all devices
				sendSearch();
				try {
					jlog.info("Waiting 5 seconds for renderer...");
					Thread.sleep(5000);
				} catch (InterruptedException e) {
				}
			}
		} while (renderer.isEmpty());
	}

	/**
	 * getDevices( String t ) Gets array of all devices with a specific service
	 * type
	 * 
	 * @param t
	 *            UDAServiceType of devices
	 * @return array of Devices
	 */
	public Device[] getDevices(final String t) {
		if (upnpService != null) {
			Collection<Device> devs = registry.getDevices(new UDAServiceType(t));
			jlog.info("getDevices: found " + devs.size() + " devices.");

			if (devs.size() > 0) {
				Device[] d = new Device[1];
				return devs.toArray(d);
			} else {
				return new Device[0];
			}
		} else {
			jlog.warn("ERROR: upnpService not initialized!");
		}
		return null;
	}

	/**
	 * getDeviceNames( String t ) Gets the names of all devices of a specific
	 * service type (t).
	 * 
	 * @param t
	 *            Service type of the devices
	 * @return String[] array of Devicenames
	 */
	public String[] getDeviceNames(final String t) {
		Device[] devs = getDevices(t);
		String[] res = new String[1];

		if (devs != null) {
			if (devs.length > 0) {
				jlog.info("GetDeviceNames: found " + devs.length + " devices.");
				res = new String[devs.length];
				for (int i = 0; i < devs.length; i++) {
					res[i] = devs[i].getDetails().getFriendlyName();
				}
			} else {
				res[0] = "No devices found.";
			}
		} else {
			res[0] = "ERROR: upnpService not initialized!";
		}
		return res;
	}

	/**
	 * getServerArray Returns a String array of all found UPNP Server names
	 * 
	 * @return String[]
	 */
	public String[] getServerArray() {
		return getDeviceNames("ContentDirectory");
	}

	/**
	 * getRendererArray() Returns an array of the names of all known renderer.
	 * 
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
		Device d = findDeviceUID(sDevice);
		if(d != null ){
			jlog.info("Found " + d.getDetails().getFriendlyName() + " to be " + sDevice);
			return d.findService(type);
		}
		Collection<Device> devs = registry.getDevices(type);
		for (Device dev : devs) {
			jlog.info("Checking " + dev.getDetails().getFriendlyName() + " to be " + sDevice);
			if (dev.getDetails().getFriendlyName().equalsIgnoreCase(sDevice)) {
				return dev.findService(type);
			}
		}
		jlog.warn("Did NOT find " + sDevice);
		return null;
	}

	public Device findDeviceUID(String uid ){
		return registry.getDevice(new UDN(uid), false);
	}
	
	public String[] devBrowseItem(final String devName, final String from) throws InterruptedException, ExecutionException {
		Device server = registry.getDevice(new UDN(devName), false);
		DirContent dirc = getDirContent(server, from);

		return dirc.getItemsS();
	}

	public String[] devBrowseDir(final String devName, final String from) throws InterruptedException, ExecutionException {
		Device server = registry.getDevice(new UDN(devName), false);
		DirContent dirc = getDirContent(server, from);
		return dirc.getDirsS();
	}

	public DirContent getDirContent(final Device server, final String from) throws InterruptedException, ExecutionException {
		Service srv = server.findService(typeContent);

		if (srv != null) {
			DirContent dirc = browseTo(srv, from);
			return dirc;
		} else {
			jlog.warn("ERR: Server " + server.getDetails().getFriendlyName() + " is no media server!");
			return null;
		}
	}

	public DirContent browseTo(Service s, final String from) throws InterruptedException, ExecutionException {
		final AtomicReference<DirContent> res = new AtomicReference<DirContent>();

		doBrowse(s, from, (ActionInvocation actionInvocation, DIDLContent didl) -> {
			if (didl != null) {
				jlog.info("browseTo: from=" + from + "\n" + printDIDL(didl) + "\n");
				res.set(new DirContent(from, from, didl.getItems(), didl.getContainers()));
			} else
				res.set(null);
			return;
		});

		DirContent ret = res.get();
		if (ret != null)
			jlog.info("browseTo: res=" + ret.getId());
		return ret;

	}

	private String printDIDL(DIDLContent d) {
		String res = "";
		res += d.getContainers().size() + " containers:\n";
		for (Container cont : d.getContainers()) {
			res += cont.getTitle() + "[" + cont.getFirstResource().getValue() + "]" + ", ";
		}
		res += "\n";

		res += d.getItems().size() + " items ";
		for (Item item : d.getItems()) {
			res += item.getTitle() + ", ";
		}
		res += "\n";

		res += d.getDescMetadata().size() + " metadata ";
		for (DescMeta m : d.getDescMetadata()) {
			res += m.getId() + ", ";
		}
		res += "\n";

		return res;
	}

	public int getDirSize(PlayJob job) throws InterruptedException, ExecutionException {
		if (job != null && job.checkJob())
			return getDirSize(findVault(job.getServer()), job.getPlaylist());
		return 0;
	}

	public int getDirSize(Service theSource, final String from) throws InterruptedException, ExecutionException {
		if (theSource == null) {
			jlog.warn("getDirSize: no source defined!");
			return 0;
		}
		if (from == null || from.length() == 0) {
			jlog.warn("getDirSize: no from defined!");
			return 0;
		}

		final AtomicReference<Integer> res = new AtomicReference<Integer>();

		doBrowse(theSource, from, (ActionInvocation actionInvocation, DIDLContent didl) -> {
			List<Item> items = didl.getItems();
			res.set(items.size());
		});

		return res.get();
	}

	protected void doBrowse(Service s, String from, BiConsumer<ActionInvocation, DIDLContent> func) throws InterruptedException, ExecutionException {

//		ActionCallback doBrowseAction = new Browse(s, from, BrowseFlag.DIRECT_CHILDREN) {
		ActionCallback doBrowseAction = new Browse(s, from, BrowseFlag.DIRECT_CHILDREN, "*", 0l, 9999l ) {
			@Override
			public void received(ActionInvocation actionInvocation, DIDLContent didl) {
				func.accept(actionInvocation, didl);
			}

			@Override
			public void updateStatus(Status status) {
				jlog.info("doBrowse: " + s.toString() + " browse status: " + status);
			}

			@Override
			public void failure(ActionInvocation invocation, UpnpResponse operation, String defaultMsg) {
				// Something wasn't right...
				jlog.warn("doBrowse: " + s.toString() + " failure: " + defaultMsg);
				DIDLContent didl = new DIDLContent();
				func.accept(actionInvocation, didl);
			}
		};
		upnpService.getControlPoint().execute(doBrowseAction).get();

		return;
	}

	public Item getItemFromServer(Service server, String playlist, int item) throws InterruptedException, ExecutionException {
		if (server == null) {
			jlog.warn("getItemFromServer: No server!");
			return null;
		}

		// Service theSource, final String from, final int no){
		final AtomicReference<Item> res = new AtomicReference<Item>();

		ActionCallback doBrowseAction = new Browse(server, playlist, BrowseFlag.DIRECT_CHILDREN, "*", item, new Long(1),
				(SortCriterion[]) null) {

			@Override
			public void received(ActionInvocation actionInvocation, DIDLContent didl) {
				List<Item> items = didl.getItems();
				if (items.size() > 0) {
					res.set(items.get(0));
				} else {
					res.set(null);
				}
			}

			@Override
			public void updateStatus(Status status) {
			}

			@Override
			public void failure(ActionInvocation invocation, UpnpResponse operation, String defaultMsg) {
				// Something wasn't right...
				jlog.warn("getItemFromServer: " + defaultMsg + " \nwhile:\nserver="
						+ server.getDevice().getDisplayString() + "\nplaylist=" + playlist + "\nitem=" + item);
				res.set(null);
			}
		};

		ctrlPoint.execute(doBrowseAction).get();

		return res.get();
	}

	public void jumpForward(String r, int n) {
		DLNACtrlPlaySimple p = mapPCtrl.get(r);
		if(p != null)
			p.jumpForward(n);
	}

	public void jumpBack(String r, int n) {
		DLNACtrlPlaySimple p = mapPCtrl.get(r);
		if(p != null)
			p.jumpBack(n);
	}


	public void stopPlay(String r) {
		DLNACtrlPlaySimple p = mapPCtrl.get(r);
		if(p != null)
			p.stop();
	}
	public void startPlay(String r) {
		DLNACtrlPlaySimple p = mapPCtrl.get(r);
		if(p != null)
			p.play();
	}

	public void setLoop(String r, boolean b) {
		DLNACtrlPlaySimple p = mapPCtrl.get(r);
		if(p != null)
			p.setLoop(b);
	}

	public void setRandom(String r, boolean b) {
		DLNACtrlPlaySimple p = mapPCtrl.get(r);
		if(p != null)
			p.setRandom(b);
	}



	protected boolean sendURIandPlay(Service theScreen, Item item) throws InterruptedException, ExecutionException {

		TransportStateCallback.add(ctrlPoint, theScreen, 10); 
		
		String uri = item.getFirstResource().getValue();
		final AtomicReference<Boolean> res = new AtomicReference<Boolean>();
		res.set(false);

		jlog.info("Send media " + uri + " to " + theScreen.getDevice().getDetails().getFriendlyName());

		ActionCallback setAVTransportURIAction = new SetAVTransportURI(theScreen, uri, "") {
			@Override
			public void failure(ActionInvocation invocation, UpnpResponse operation, String defaultMsg) {
				// Something was wrong
				jlog.error("Send media " + uri + " to "
						+ theScreen.getDevice().getDetails().getFriendlyName() + " failed with msg:"+defaultMsg);
				jlog.error("details are: action=" + invocation.getAction().getName());
				jlog.error("             ClientInfo=" + invocation.getClientInfo());
				jlog.error("             op.response=" + operation.getResponseDetails());
				res.set(true);
			}
		};
		// fa = upnpService.getControlPoint().execute(setAVTransportURIAction);
		ctrlPoint.execute(setAVTransportURIAction).get();

		if (res.get()) {
			return false;
		}

		jlog.info("Playing media " + uri + " on " + theScreen.getDevice().getDetails().getFriendlyName());
		ActionCallback playAction = new Play(theScreen) {
			@Override
			public void failure(ActionInvocation invocation, UpnpResponse operation, String defaultMsg) {
				// Something was wrong
				jlog.warn("Playing media " + uri + " on "
						+ theScreen.getDevice().getDetails().getFriendlyName() + " failed!");
				res.set(true);
			}
		};
		// fa = upnpService.getControlPoint().execute(playAction);
		ctrlPoint.execute(playAction).get();

		return !res.get();
	}

	/**
	 * Play job/playlist
	 * 
	 * @param sRenderer
	 * @param sDir
	 * @param theId
	 * @param duration
	 */
	public void play(String sRenderer, String sDir, String theId, int duration, int no, boolean bLoop, boolean bRandom) {
		PlayJob pj = new PlayJob("starting", theId, sRenderer, sDir, no, duration, duration, duration);
		pj.setNextRandom(bRandom);
		pj.setLoop(bLoop);
		play(pj);
	}
	public void saveJob(String name, String sRenderer, String sDir, String theId, int duration, int no, boolean bLoop, boolean bRandom) {
		PlayJob pj = new PlayJob("starting", theId, sRenderer, sDir, no, duration, duration, duration);
		pj.setNextRandom(bRandom);
		pj.setLoop(bLoop);
		saveJob(name, pj);
	}
	public void saveJob(String name, String sRenderer){
		DLNACtrlPlaySimple ctrl = mapPCtrl.get(sRenderer);
		if( ctrl != null){
			PlayJob pj = ctrl.getJob();
			if(pj != null){
				saveJob(name, pj);
				return;
			}
		}

		jlog.error("Attempting to save playjob: "+name + " for Renderer: "+sRenderer + ", but no job playing at renderer.");
	}
	private void saveJob(String name, PlayJob pj){
		mapPJobs.put(name, pj);
		jlog.warn("Saved PlayJob " + name + " for " + pj.getScreen());
	}
	public void play(String name){
		PlayJob pj = mapPJobs.get(name);
		if(pj != null){
			jlog.warn("Starting saved PlayJob " + name + " for " + pj.getScreen());
			play(pj);
		}
		else{
			jlog.error("Attempting to start missing saved playjob: " + name);
		}
	}

	private void play(PlayJob job) {
		jlog.info("Play " + job.getPlaylist() + " from " + job.getServer() + " to " + job.getScreen());

		if (!job.checkJob()) {
			jlog.error("Cant play bogus job.....");
			return;
		}

		if (job.getPictTime() < 5000) {
			jlog.warn("Duration below 5 seconds! {" + job.getPictTime() + "}");
			return;
		}
		
		DLNACtrlPlaySimple p = mapPCtrl.get(job.getScreen());
		if(p == null){
			p = new DLNACtrlPlaySimple(this, eSrvc);
			mapPCtrl.put(job.getScreen(), p);
			jlog.warn("Added DLNACtrlPlaySimple for " + job.getScreen());
		}
		p.startPlayThread(job);
	}

}