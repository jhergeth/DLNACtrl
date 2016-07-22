package name.hergeth.dlna.core;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

public class PlayJob {
	static private ObjectMapper mapper = new ObjectMapper(); // can reuse, share globally;
	static private Logger jlog = LoggerFactory.getLogger("name.hergeth.dlna.core");

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
	
	int step;

	public String 	getStatus() {					return status;	}
	public void 	setStatus(String status) {		this.status = status;	}
	public String 	getPlaylist() {				return playlist;	}
	public void 	setPlaylist(String medium) {	this.playlist = medium;	}
	public String 	getScreen() {					return screen;	}
	public void 	setScreen(String screen) {		this.screen = screen;	}
	public String 	getServer() {					return server;	}
	public void 	setServer(String server) {		this.server = server;	}
	public String 	getItemTitle() {				return itemTitle;	}
	public void 	setItemTitle(String itemTitle) {this.itemTitle = itemTitle;	}
	public String 	getItemPath() {				return itemPath;	}
	public void 	setItemPath(String itemPath) {	this.itemPath = itemPath;	}
	public int 		getItemNo() {					return itemNo;	}
	public void 	setItemNo(int item) {			if (item >= 0) { this.itemNo = item; } else {
						jlog.error("Trying to set negative item number: " + item);	}	}
	public int 		getListLength() {				return listLength;	}
	public void 	setListLength(int listLength) {	this.listLength = listLength;	}
	public int 		getTotalTime() {					return totalTime;	}
	public void 	setTotalTime(int totalTime) {	this.totalTime = totalTime;	}
	public int 		getRestTime() {					return restTime;	}
	public void 	setRestTime(int restTime) {		this.restTime = restTime;	}
	public int 		getPictTime() {					return pictTime;	}
	public void 	setPictTime(int pictTime) {		this.pictTime = pictTime;	}

	
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
		step = 1;
	}

	public PlayJob() {
		status = "idle";
		playlist = screen = server = "";
		itemNo = totalTime = restTime = 0;
		step = 1;
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
		step = 1;
	}
	
	public static PlayJob read(String f){
		try {
			return( mapper.readValue(new File(f), PlayJob.class));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}

	public void save(String f){
		if(screen.length() > 0 ){
			String fn = f + "/DLNA_" + screen + ".json";
			try {
				mapper.writeValue(new File(fn), this);
				jlog.info("Writing save file to: " + fn);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				jlog.info("Exception during writing save file to: " + fn);
				e.printStackTrace();
			}
		}
	}

	public boolean hasStatus(String s) {
		return (status.compareToIgnoreCase(s) == 0);
	}

	public void jumpForward(int s) {
		step = s+1;
		restTime = 0;
	}

	public void jumpBack(int s) {
		step = -s;
		restTime = 0;
	}

	public void jumpClear() {
		step = 1;
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
