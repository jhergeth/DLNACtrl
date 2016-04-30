package name.hergeth.dlna.core;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.fourthline.cling.controlpoint.ActionCallback;
import org.fourthline.cling.controlpoint.SubscriptionCallback;
import org.fourthline.cling.model.action.ActionInvocation;
import org.fourthline.cling.model.message.UpnpResponse;
import org.fourthline.cling.model.meta.Service;
import org.fourthline.cling.support.avtransport.callback.Play;
import org.fourthline.cling.support.avtransport.callback.SetAVTransportURI;
import org.fourthline.cling.support.model.item.Item;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DLNACtrlPlaySimple {
	private enum s {
		running, idle
	};

	private PlayJob newJob = null;
	private PlayJob job = null;
	private DLNACtrl ctrl = null;
	private s status = s.idle;
	private Logger jlog = LoggerFactory.getLogger("name.hergeth.dlna.core");

	public DLNACtrlPlaySimple(DLNACtrl c) {
		this.ctrl = c;
		this.job = null;
		status = s.idle;
	}

	public void setJob(PlayJob job) {
		if (status == s.idle)
			this.job = job;
		else {
			newJob = job;
			status = s.idle;
			job.setRestTime(0);
		}
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
			job.setRestTime(Integer.MAX_VALUE);
		}
	}

	public void play() {
		if (job.checkJob() && status != s.idle) {
			job.setRestTime(job.getTotalTime());
		}
	}

	public void restart(String devName) {
		if (job != null && status != s.idle) {

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

	public void doPlay() throws InterruptedException, ExecutionException {
		if (job == null)
			return;

		jlog.info("Starting rendering of playlist " + job.getPlaylist());

		int dirsize = 0;
		int m = job.itemNo;
		do {

			job.setItemNo(m);

			Service<?, ?> theScreen = ctrl.findRenderer(job.getScreen()); // find
																			// a
																			// renderer
			jlog.warn("rendering service " + (theScreen == null ? "not" : "") + " found!");

			Service<?, ?> theSource = ctrl.findVault(job.getServer()); // find
																		// media
																		// vault
			jlog.warn("media vault " + (theSource == null ? "not" : "") + " found!");

			dirsize = ctrl.getDirSize(theSource, job.getPlaylist());
			int cnt = 0;
			while ((theScreen == null || theSource == null || dirsize == 0) && !ctrl.getExecPlay().isShutdown()) {
				if (theScreen == null) {
					jlog.warn("No rendering service found!");
					job.setStatus("renderer mising");
				}
				if (theSource == null) {
					jlog.warn("No media directory found!");
					job.setStatus("media directory mising");
				}

				if (cnt % 12 == 0) {
					// Broadcast a search message for all devices
					ctrl.sendSearch();
				}

				jlog.info("Waiting 5 seconds for devices...");
				Thread.sleep(5000);
				if (job.hasStatus("stop")) {
					status = s.idle;
					return;
				}
				if (newJob != null) {
					job = newJob;
					newJob = null;
					return;
				}
				theScreen = ctrl.findRenderer(job.getScreen()); // find a
																// renderer
				theSource = ctrl.findVault(job.getServer()); // find media vault
				ctrl.browseTo(theSource, job.getPlaylist());
				dirsize = ctrl.getDirSize(theSource, job.getPlaylist());
				cnt++;
			}
			if (ctrl.getExecPlay().isShutdown())
				return;

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
			if (job.hasStatus("stop")) {
				status = s.idle;
			}

			if (newJob != null) {
				job = newJob;
				newJob = null;
				return;
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
		} while (m < dirsize && !ctrl.getExecPlay().isShutdown());

		status = s.idle;
		jlog.info("Rendering of playlist " + job.getPlaylist() + " finished..");
	}

	public void playItemOnScreen(Service theScreen, Item item, int duration)
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

		while (time > 0 && !ctrl.getExecPlay().isShutdown()) {
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

}