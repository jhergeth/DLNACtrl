package name.hergeth.dlna.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PlayJob {
	private Logger jlog = LoggerFactory.getLogger("name.hergeth.dlna.core");

	String status;
	String playlist;
	String screen;
	String server;
	String itemTitle;
	String itemPath;
	int itemNo;
	int listLength;
	int totalTime;
	int restTime;
	int pictTime;
	boolean back;
	boolean forward;

	public PlayJob(String status, String playlist, String screen, String server, int item, int total, int rest,
			int picTime) {
		this.status = status;
		this.playlist = playlist;
		this.screen = screen;
		this.server = server;
		this.listLength = 0;
		this.totalTime = total;
		this.restTime = rest;
		this.itemNo = item;
		this.pictTime = picTime;
		back = forward = false;
	}

	public PlayJob() {
		status = "idle";
		playlist = screen = server = "";
		itemNo = totalTime = restTime = 0;
		back = forward = false;
	}

	public PlayJob(PlayJob o) {
		this.status = o.status;
		this.playlist = o.playlist;
		this.screen = o.screen;
		this.server = o.server;
		this.totalTime = o.totalTime;
		this.restTime = o.restTime;
		this.itemNo = o.itemNo;
		this.pictTime = o.pictTime;
		back = forward = false;
	}

	public boolean hasStatus(String s) {
		return (status.compareToIgnoreCase(s) == 0);
	}

	public void jumpForward() {
		forward = true;
		restTime = 0;
	}

	public int getItemNo() {
		return itemNo;
	}

	public int getListLength() {
		return listLength;
	}

	public void setListLength(int listLength) {
		this.listLength = listLength;
	}

	public int getTotalTime() {
		return totalTime;
	}

	public void setTotalTime(int totalTime) {
		this.totalTime = totalTime;
	}

	public int getRestTime() {
		return restTime;
	}

	public void setRestTime(int restTime) {
		this.restTime = restTime;
	}

	public void jumpBack() {
		back = true;
		restTime = 0;
	}

	public void jumpClear() {
		back = forward = false;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
		// DLNAInit.jlog.log(Level.INFO, "New status of job " + playlist + " on
		// " +
		// screen + ": " + status);
	}

	public String getItemTitle() {
		return itemTitle;
	}

	public void setItemTitle(String itemTitle) {
		this.itemTitle = itemTitle;
	}

	public String getItemPath() {
		return itemPath;
	}

	public void setItemPath(String itemPath) {
		this.itemPath = itemPath;
	}

	public String getPlaylist() {
		return playlist;
	}

	public void setPlaylist(String medium) {
		this.playlist = medium;
	}

	public String getScreen() {
		return screen;
	}

	public void setScreen(String screen) {
		this.screen = screen;
	}

	public String getServer() {
		return server;
	}

	public void setServer(String server) {
		this.server = server;
	}

	public void setItemNo(int item) {
		if (item >= 0) {
			this.itemNo = item;
		} else {
			jlog.error("Trying to set negative item number: " + item);
		}
	}

	public int getPictTime() {
		return pictTime;
	}

	public void setPictTime(int pictTime) {
		this.pictTime = pictTime;
	}

	public boolean checkJob() {
		if (getServer() == null) {
			jlog.warn("No source defined!");
			return false;
		}
		if (getScreen() == null) {
			jlog.warn("No renderer defined!");
			return false;
		}
		if (getPlaylist() == null) {
			jlog.warn("Empty playlist!");
			return false;
		}

		return true;
	}

}
