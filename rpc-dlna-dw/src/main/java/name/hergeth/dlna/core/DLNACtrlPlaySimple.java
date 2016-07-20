package name.hergeth.dlna.core;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.fourthline.cling.model.meta.Service;
import org.fourthline.cling.support.model.item.Item;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DLNACtrlPlaySimple {
	private enum s {
		running, idle, starting
	};
	public ExecutorService execPlay;
	private Future<?> waitForPlay = null;

	private PlayJob newJob = null;
	private PlayJob job = null;
	private DLNACtrl ctrl = null;
	private s status = s.idle;
	private Logger jlog = LoggerFactory.getLogger("name.hergeth.dlna.core");

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
			job.setRestTime(0);
		}
		status = s.idle;
	}

	public PlayJob getJob() {
		return job;
	}

	public void jumpForward() {
		if (job.checkJob() && status != s.idle) {
			job.jumpForward();
		}
	}

	public void jumpBack() {
		if (job.checkJob() && status != s.idle) {
			job.jumpBack();
		}
	}

	public void stop() {
		if (job.checkJob() && status != s.idle) {
			job.setRestTime(0);
		} else {
			job = null;
			status = s.idle;
		}
	}

	public void play() {
		if (job.checkJob() && status != s.idle) {
			job.setRestTime(job.getTotalTime());
		}
	}

	public void restart(String devName) {
		if (job != null && status != s.idle) {
			int t = job.getTotalTime() - job.getRestTime();
			job.setRestTime(t > 0 ? t : 0);
		}
	}

	public boolean isRunning() {
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

	@SuppressWarnings("rawtypes")
	public void doPlay() throws InterruptedException, ExecutionException {
		if (job == null)
			return;

		jlog.info("Starting rendering of playlist " + job.getPlaylist());

		Service theScreen = null;
		Service theSource = null;
		int dirsize = 0;
		int m = job.itemNo;
		do {		// loop through all items in the playlist

			job.setItemNo(m);

			int cnt = 0;
			do { // get all needed devices
				theScreen = ctrl.findRenderer(job.getScreen()); // find a renderer
				if (theScreen == null) {
					jlog.warn("No rendering service found!");
					job.setStatus("renderer mising");
				}

				theSource = ctrl.findVault(job.getServer()); // find media vault
				if (theSource == null) {
					jlog.warn("No media directory found!");
					job.setStatus("media directory mising");
				}

				dirsize = ctrl.getDirSize(theSource, job.getPlaylist());
				if ((theScreen == null || theSource == null || dirsize == 0)) {

					if (cnt % 10 == 0) {
						ctrl.sendSearch();	// Broadcast a search message for all devices every 20 seconds
					}
					if (execPlay.isShutdown())
						return;				// the whole thread is shutting down, .....

					jlog.info("Waiting 2 seconds for devices...");
					Thread.sleep(2000);
					if (newJob != null) {	// oh´, there is a new playjob, lets do that
						job = newJob;
						newJob = null;
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

			job.setListLength(dirsize);
			job.setItemNo(m % dirsize);
			m = job.getItemNo();
			Item item = ctrl.getItemFromServer(theSource, job.getPlaylist(), m);
			jlog.info("Rendering service and media diretory and item found!");
			job.setItemTitle(item.getTitle());
			job.setItemPath(item.getFirstResource().getValue());
			jlog.info(".... now rendering " + job.getItemTitle() + " from " + job.getPlaylist() + " to "
					+ job.getScreen());

			status = s.running;
			job.setStatus("startPlay");
			playItemOnScreen(theScreen, item, job.getPictTime());

			if (execPlay.isShutdown())
				return;				// the whole thread is shutting down, .....
			if (job.hasStatus("stop")) {
				status = s.idle;	// we shall stop the playlist
				return;
			}

			if (newJob != null) {	// change of plans, new playlist has arrived
				job = newJob;
				newJob = null;
			}
			if (job.hasStatus("medium finished")) {
				m = job.getItemNo();
				if (job.back && m > 1) {
					m -= 2;
				}
				job.jumpClear();
			} else { // something went wrong ....
				m--;
				// Broadcast a search message for all devices
				ctrl.sendSearch();
			}

			m++;
		} while (m <= dirsize);
		
		job.setItemNo(1); //point to start of list.

		status = s.idle;
		jlog.info("Rendering of playlist " + job.getPlaylist() + " finished..");
	}

	public void playItemOnScreen(@SuppressWarnings("rawtypes") Service theScreen, Item item, int duration)
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
	
	public void startPlayThread(PlayJob job) {
		if (waitForPlay == null || waitForPlay.isDone()) {
			setJob(job);
	
			startPlayThread();
		} else {
			jlog.info("Rendering is already running, attempting to change playlist.");
			setJob(job);
		}
	}

	public void startPlayThread() {
		waitForPlay = execPlay.submit(() -> {
			jlog.info("Starting play job...");
			while (!execPlay.isShutdown()) {
				try {
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

}