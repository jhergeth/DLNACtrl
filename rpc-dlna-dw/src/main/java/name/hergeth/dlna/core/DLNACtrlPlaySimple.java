package name.hergeth.dlna.core;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.fourthline.cling.model.meta.Service;
import org.fourthline.cling.support.model.item.Item;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import name.hergeth.dlna.DLNAApplication;

public class DLNACtrlPlaySimple {
	private enum s {
		idle, getDevices, waitForDevices, sendItem, sleep, nextItem, running
	};
	public ExecutorService execPlay;
	private Future<?> waitForPlay = null;

	private PlayJob newJob = null;
	private PlayJob job = null;
	private DLNACtrl ctrl = null;
	private s status = s.idle;
	private Logger jlog = LoggerFactory.getLogger("name.hergeth.dlna.core");
	
	private Service sm_theScreen = null;
	private Service sm_theSource = null;
	private int sm_dirsize = 0;
	private int sm_itemNo = 0;
	private int sm_cnt = 0;


	public DLNACtrlPlaySimple(DLNACtrl c, ExecutorService esrv) {
		execPlay = esrv;

		this.ctrl = c;
		this.job = null;
		status = s.idle;
	}
	
	public void end()
	{
		// Shutdown executors
		try {
			jlog.info("attempt to shutdown executor");
			execPlay.shutdown();
			execPlay.awaitTermination(11, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			jlog.warn("tasks interrupted");
		} finally {
			if (!execPlay.isTerminated()) {
				jlog.warn( "cancel non-finished tasks");
			}
			execPlay.shutdownNow();
			jlog.info( "shutdown finished");
		}

		execPlay = null;
	}

	public void setJob(PlayJob job) {
		if (status == s.idle)
			this.job = job;
		else {
			newJob = job;
			this.job.setRestTime(0);
		}
	}

	public PlayJob getJob() {
		return job;
	}

	public void jumpForward(int n) {
		if (job.checkJob() && isRunning()) {
			job.jumpForward(n);
		}
	}

	public void jumpBack(int n) {
		if (job.checkJob() && isRunning()) {
			job.jumpBack(n);
		}
	}

	public void stop() {
		if (job.checkJob() && isRunning()) {
			job.setRestTime(0);
		} else {
			job = null;
			status = s.idle;
		}
	}

	public void play() {
		if (job.checkJob() && isRunning()) {
			job.setRestTime(job.getTotalTime());
		}
	}

	public void restart(String devName) {
		if (job != null && isRunning()) {
			int t = job.getTotalTime() - job.getRestTime();
			job.setRestTime(t > 0 ? t : 0);
		}
	}

	public boolean isRunning() {
		return status != s.idle;
	}

	public void startPlayThread(PlayJob job) {
		if (waitForPlay == null || waitForPlay.isDone()) {
			setJob(job);
	
			startPlayThread();
			jlog.info("Starting PlayThread for renderer " + job.getScreen());
		} else {
			jlog.info("Rendering is already running, attempting to change playlist.");
			setJob(job);
		}
	}

	public void startPlayThread() {
		jlog.info("Preparing thread submit call for renderer "+job.getScreen());
		waitForPlay = execPlay.submit(() -> {
			jlog.info("Starting play job for renderer "+job.getScreen());
			while (!execPlay.isShutdown()) {
				try {
					jlog.info("Will call doPlay for renderer "+job.getScreen());
					doPlay();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					jlog.info("Play aborted due to exception.");
					stop();
				}
				if ((!isRunning()) || (getJob() == null )) {
					try {
						execPlay.wait(1000);
					} catch (Exception e) {
					}
					jlog.info("idle loop....");
				}
				else{
					jlog.info("Play rollover....");
				}
			}
			waitForPlay = null;
		});
	}

/*	
	public void startPlayThread() {
		status = s.getDevices;
		jlog.info("Preparing thread submit call for renderer "+job.getScreen());
		while (!execPlay.isShutdown()) {
			jlog.info("Looping through job for renderer "+job.getScreen()+" status:"+status);
			waitForPlay = execPlay.submit(this);
			try {
				waitForPlay.get();
			} catch (InterruptedException | ExecutionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	@Override
	public void run(){
		try {
			switch( status){
				case idle:
					doIdle();
				break;
				case getDevices:
					doGetDevices();
				break;
				case waitForDevices :
					doWaitForDevices();
				break;
				case sendItem:
					doSendItem();
				break;
				case sleep:
					doSleep();
				break;
				case nextItem:
					doNextItem();
				break;
			}	
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			jlog.info("Play aborted due to exception.");
			stop();
		}
	}
	
	private void doIdle(){
		jlog.info("idle loop....");

	}

	private void doGetDevices(){

	}

	private void doWaitForDevices(){

	}

	private void doSendItem(){

	}

	private void doSleep(){

	}

	private void doNextItem(){

	}

	private void startTimer(){
		
	}
	}
	
	
				
				() -> {
					jlog.info("Starting play job for renderer "+job.getScreen());
			while (!execPlay.isShutdown()) {
				try {
					jlog.info("Will call doPlay for renderer "+job.getScreen());
					doPlay();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					jlog.info("Play aborted due to exception.");
					stop();
				}
				if ((!isRunning()) || (getJob() == null )) {
					try {
						execPlay.wait(1000);
					} catch (Exception e) {
					}
					jlog.info("idle loop....");
				}
				else{
					jlog.info("Play rollover....");
				}
			}
			waitForPlay = null;
		});
	}
*/
	
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

	@SuppressWarnings("rawtypes")
	private void doPlay() throws InterruptedException, ExecutionException {
		if (job == null)
			return;

		jlog.info("Starting rendering of playlist " + job.getPlaylist());

		do {		// loop through all items in the playlist

			int cnt = 0;
			do { // get all needed devices
				sm_theScreen = ctrl.findRenderer(job.getScreen()); // find a renderer
				if (sm_theScreen == null) {
					jlog.warn("No rendering service found!");
					job.setStatus("renderer mising");
				}

				sm_theSource = ctrl.findVault(job.getServer()); // find media vault
				if (sm_theSource == null) {
					jlog.warn("No media directory found!");
					job.setStatus("media directory mising");
				}

				if (sm_theScreen == null || sm_theSource == null) {

					if (cnt % 10 == 0) {
						jlog.info("Searching UPNP ...");
						ctrl.sendSearch();	// Broadcast a search message for all devices every 20 seconds
					}
					if (execPlay.isShutdown())
						return;				// the whole thread is shutting down, .....

					jlog.info("Waiting 2 seconds for devices...");
					Thread.sleep(2000);
					if (newJob != null) {	// oh´, there is a new playjob, lets do that
						job = newJob;
						newJob = null;
						job.setItemNo(0);
					}
					if (job.hasStatus("stop")) {
						status = s.idle;	// we are done here
						return;
					}

					cnt++;
				}
				else	// we found all devices, so go on with using them
					break;

			} while (true);

			sm_dirsize = ctrl.getDirSize(sm_theSource, job.getPlaylist());
			if( sm_dirsize == 0 ){
				job.setStatus("stop");
				status = s.idle;
				return;
			}
			job.setListLength(sm_dirsize);
			job.setItemNo(sm_itemNo % sm_dirsize);
			sm_itemNo = job.getItemNo();
			Item item = ctrl.getItemFromServer(sm_theSource, job.getPlaylist(), sm_itemNo);
			jlog.info("Rendering service and media diretory and item found!");
			job.setItemTitle(item.getTitle());
			job.setItemPath(item.getFirstResource().getValue());
			jlog.info(".... now rendering " + job.getItemTitle() + " from " + job.getPlaylist() + " to "
					+ job.getScreen());

			status = s.running;
			job.setStatus("startPlay");
			playItemOnScreen(sm_theScreen, item, job.getPictTime());

			if (execPlay.isShutdown())
				return;				// the whole thread is shutting down, .....
			if (job.hasStatus("stop")) {
				status = s.idle;	// we shall stop the playlist
				return;
			}

			if (newJob != null) {	// change of plans, new playlist has arrived
				job = newJob;
				newJob = null;
				job.setItemNo(0);
			}

			if (job.hasStatus("medium finished")) {
				sm_itemNo = job.getItemNo();
				sm_itemNo += job.step;
				job.jumpClear();
				if ( sm_itemNo < 0) {
					sm_itemNo += sm_dirsize;
				}
				if( sm_itemNo >= sm_dirsize){
					sm_itemNo -= sm_dirsize;
				}
				
				job.setItemNo(sm_itemNo);
			} else { // something went wrong ....
				// Broadcast a search message for all devices
				ctrl.sendSearch();
			}


		} while (sm_itemNo <= sm_dirsize);
		
		job.setItemNo(0); //point to start of list.

		status = s.idle;
		jlog.info("Rendering of playlist " + job.getPlaylist() + " finished..");
	}

	private void playItemOnScreen(@SuppressWarnings("rawtypes") Service theScreen, Item item, int duration)
			throws InterruptedException, ExecutionException {
		String uri = item.getFirstResource().getValue();

		if (!ctrl.sendURIandPlay(theScreen, item)) {
			return;
		}

		long time = duration;
		String dur = item.getFirstResource().getDuration();
		if (dur != null) {
			jlog.info("Duration of " + uri + " is: " + dur);
			time = timeToLong(dur) + 1500;
		}

		if (time < 5000)
			time = 5000;

		job.setStatus("playing");
		job.setTotalTime((int) (time));
		job.setRestTime((int) (time));
		jlog.info("Sleeping " + time / 1000 + " seconds media=" + uri);

		while (time > 0 ) {
			if (execPlay.isShutdown())
				return;				// the whole thread is shutting down, .....

			try {
				job.setRestTime((int) (time - 1000));
				job.save(DLNAApplication.Config().getDefaultSavePath());
				Thread.sleep(1000);
			} catch (InterruptedException e) {
			}

			if (job.hasStatus("screen restarted")) {
				jlog.info("Resend URI after screen restart " + job.getRestTime() + " seconds media=" + uri);
				if (!ctrl.sendURIandPlay(theScreen, item)) {
					return;
				}
			}
			job.setStatus("playing");
			time = job.getRestTime();
			jlog.info("Waiting " + time / 1000 + "." + time % 1000 + " seconds media=" + uri);
		}
		job.setStatus("medium finished");
		job.setRestTime(0);
	}



}