package mctrl;

import java.util.concurrent.ExecutionException;
import java.util.logging.Level;

import org.fourthline.cling.controlpoint.ActionCallback;
import org.fourthline.cling.controlpoint.ControlPoint;
import org.fourthline.cling.model.action.ActionInvocation;
import org.fourthline.cling.model.message.UpnpResponse;
import org.fourthline.cling.model.message.header.STAllHeader;
import org.fourthline.cling.model.meta.Service;
import org.fourthline.cling.support.avtransport.callback.Play;
import org.fourthline.cling.support.avtransport.callback.SetAVTransportURI;
import org.fourthline.cling.support.model.item.Item;

public class DLNACtrlPlaySimple {
	private enum s { running, idle };
	private PlayJob newJob = null;
	private PlayJob job = null;
	private DLNACtrl ctrl = null;
	private s status = s.idle;

	public DLNACtrlPlaySimple(DLNACtrl c) {
		this.ctrl = c;
		this.job = null;
		status = s.idle;
	}
	
	public void setJob( PlayJob job ){
		if(status == s.idle)
			this.job = job;
		else
			newJob = job;
	}
	
	public PlayJob getJob(){
		return job;
	}

	public void jumpForward() {
		if(job.checkJob() && status != s.idle){
			job.jumpForward();
		}
	}

	public void jumpBack() {
		if(job.checkJob() && status != s.idle){
			job.jumpBack();
		}
	}

	public void stop() {
		if(job.checkJob() &&status != s.idle){
			job.setRest(Integer.MAX_VALUE);
		}
	}

	public void play() {
		if(job.checkJob() && status != s.idle){
			job.setRest(job.getTotal());
		}
	}
	
	public void restart(String devName){
		if(job != null && status != s.idle){
			
		}
	}
	
	public boolean isRunning(){
		return status != s.idle;
	}

	
	private long timeToLong(String s) {
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

	
	public void doPlay() {
		if(job == null)
			return;
		
		Main.jlog.log(Level.INFO, "Starting rendering of playlist " + job.getPlaylist());

		int dirsize = 0;
		int m = 0;
		do{
		
			job.setItem(m);

			Service<?, ?> theScreen = ctrl.findRenderer(job.getScreen());		// find a renderer
			Service<?, ?> theSource = ctrl.findVault(job.getServer());;		// find media vault
			Item item = ctrl.getItemFromServer( theSource, job.getPlaylist(), m );
			dirsize = ctrl.getDirSize(theSource, job.getPlaylist());
			
			
			int cnt = 0;
			while(theScreen == null || theSource == null || item == null){
				if( theScreen == null ){
					Main.jlog.log(Level.WARNING, "No rendering service found!");
					job.setStatus("renderer mising");
				}
				if( theSource == null ){
					Main.jlog.log(Level.WARNING, "No media directory found!");
					job.setStatus("media directory mising");
				}
				if(item == null){								// no media to render!!
					Main.jlog.log(Level.WARNING, "No item found!");
					job.setStatus("item mising");
				}

				if(cnt%12 == 0){
					// Broadcast a search message for all devices
					ctrl.sendSearch();
				}
				try {
					Main.jlog.log(Level.INFO, "Waiting 5 seconds for devices...");
					Thread.sleep(5000);
					if(job.hasStatus("stop")){
						status = s.idle;
						return;
					}
					
				} catch (InterruptedException e) {
				}
				theScreen = ctrl.findRenderer(job.getScreen());		// find a renderer
				theSource = ctrl.findVault(job.getServer());;		// find media vault
				ctrl.browseTo(theSource, job.getPlaylist());
				item = ctrl.getItemFromServer( theSource, job.getPlaylist(), m );
				dirsize = ctrl.getDirSize(theSource, job.getPlaylist());
				cnt++;
			}
			Main.jlog.log(Level.INFO, "Rendering service and media diretory and item found!");
			job.setItemTitle(item.getTitle());
			job.setItemPath(item.getFirstResource().getValue());
			Main.jlog.log(Level.INFO, ".... now rendering "+job.getItemTitle() + " from " + job.getPlaylist() + " to " + job.getScreen());

			try {										// render media on renderer
				status = s.running;
				job.setStatus("startPlay");
				playItemOnScreen(theScreen, item, job.getPictTime());
				if(job.hasStatus("stop")){
					status = s.idle;
				}
				
			} catch (InterruptedException | ExecutionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				job.setStatus("exception during play");
			}

			if(newJob != null){
				job = newJob;
				newJob = null;
				return;
			}
			if( job.hasStatus("medium finished")){
				m = job.getItem();
				if(job.back && m > 1){
					m -= 2;
				}
				job.jumpClear();
			}
			else
				m--;
			
			m++;
		}while(m < dirsize);

		status = s.idle;
		Main.jlog.log(Level.INFO, "Rendering of playlist " + job.getPlaylist() + " finished..");
	}

	public void playItemOnScreen(Service theScreen, Item item, int duration)
			throws InterruptedException, ExecutionException {
		
		TransportStateCallback.add(theScreen, 10);

		String uri = sendURIandPlay(theScreen, item);
		if(!job.hasStatus("sendPlay")){
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

		job.setStatus("playing");
		job.setTotal((int)(time));
		job.setRest((int)(time));
		Main.jlog.log(Level.INFO, "Sleeping " + time/1000 + " seconds media=" + uri);

		while(time > 0){
			try {
				job.setRest((int)(time-1000));
				Thread.sleep(1000);
			} catch (InterruptedException e) {}

			if(job.hasStatus("screen restarted")){
				Main.jlog.log(Level.INFO, "Resend URI after screen restart " + job.getRest() + " seconds media=" + uri);
				uri = sendURIandPlay(theScreen, item);
			}
			job.setStatus("playing");
			time = job.getRest();
			Main.jlog.log(Level.INFO, "Waiting " + time + " seconds media=" + uri);
		}
		job.setStatus("medium finished");
		job.setRest(0);
	}

	private String sendURIandPlay(Service theScreen, Item item) throws InterruptedException, ExecutionException {
		String uri = item.getFirstResource().getValue();
		String res = null;

		Main.jlog.log(Level.INFO, "Send media " + uri + " to " + theScreen.getDevice().getDetails().getFriendlyName());

		job.setStatus("sendURL");
		ActionCallback setAVTransportURIAction =
				new SetAVTransportURI(theScreen, uri, "NO METADATA") {
			@Override
			public void failure(ActionInvocation invocation, UpnpResponse operation, String defaultMsg) {
				// Something was wrong
				job.setStatus("failure during SetAVTransport");
				Main.jlog.log(Level.WARNING, "Send media " + uri + " to " + theScreen.getDevice().getDetails().getFriendlyName() + " failed!");
				status = s.idle;
			}
		};
//		fa = upnpService.getControlPoint().execute(setAVTransportURIAction);
		ctrl.execAction(setAVTransportURIAction).get();

		if(!job.hasStatus("sendURL")){
			return uri;
		}

		Main.jlog.log(Level.INFO, "Playing media " + uri + " on " + theScreen.getDevice().getDetails().getFriendlyName());
		job.setStatus("sendPlay");
		ActionCallback playAction =
				new Play(theScreen) {
			@Override
			public void failure(ActionInvocation invocation, UpnpResponse operation, String defaultMsg) {
				// Something was wrong
				job.setStatus("failure during sendPlay");
				Main.jlog.log(Level.WARNING, "Playing media " + uri + " on " + theScreen.getDevice().getDetails().getFriendlyName() + " failed!");
				status = s.idle;
			}
		};
//		fa = upnpService.getControlPoint().execute(playAction);
		ctrl.execAction(playAction).get();

		return uri;
	}

}