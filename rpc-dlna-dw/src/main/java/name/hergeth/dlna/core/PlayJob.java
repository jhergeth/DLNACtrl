package name.hergeth.dlna.core;

import java.io.File;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
	int totalTime;
	int restTime;
	int pictTime;
	boolean bNextRandom;
	boolean bLoop;
	
	int step;

	private int itemCount;
	private int listLength;
	@JsonIgnore
	private int[] seq = null;

	public String 	getStatus() {					return status;	}
	public void 	setStatus(String status) {		this.status = status;	}
	public String 	getPlaylist() {					return playlist;	}
	public void 	setPlaylist(String medium) {	this.playlist = medium;	}
	public String 	getScreen() {					return screen;	}
	public void 	setScreen(String screen) {		this.screen = screen;	}
	public String 	getServer() {					return server;	}
	public void 	setServer(String server) {		this.server = server;	}
	public String 	getItemTitle() {				return itemTitle;	}
	public void 	setItemTitle(String itemTitle) {this.itemTitle = itemTitle;	}
	public String 	getItemPath() {					return itemPath;	}
	public void 	setItemPath(String itemPath) {	this.itemPath = itemPath;	}
	public int 		getItemNo() {					return itemNo;	}
	public void 	clearItemNo() {					this.itemNo = 0; }
	public int 		getTotalTime() {				return totalTime;	}
	public void 	setTotalTime(int totalTime) {	this.totalTime = totalTime;	}
	public int 		getRestTime() {					return restTime;	}
	public void 	setRestTime(int restTime) {		this.restTime = restTime;	}
	public int 		getPictTime() {					return pictTime;	}
	public void 	setPictTime(int pictTime) {		this.pictTime = pictTime;	}
	public boolean 	isNextRandom() {				return bNextRandom;		}
	public void 	setNextRandom(boolean bNextRandom) {	this.bNextRandom = bNextRandom;	}
	public boolean 	isLoop() {						return bLoop;	}
	public void 	setLoop(boolean bLoop) {		this.bLoop = bLoop;		}
	public int 		getListLength() {				return listLength;	}
	public void		setListLength(int listLength) {	this.listLength = listLength;	}

	
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
		bNextRandom = false;
		step = 1;
		itemCount = 0;
	}

	public PlayJob() {
		this("idle", "", "", "", 0, 0, 0, 0);
	}

	public PlayJob(PlayJob o) {
		this(o.status, o.playlist, o.screen, o.server, o.itemNo, o.totalTime, o.restTime, o.pictTime);
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
		step = s;
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
	
	public int nextItem(int dirSize){
		
		itemCount += step;
		itemNo += step;
		listLength = dirSize;

		if(isNextRandom() && listLength > 0){
			if((seq == null || seq.length != listLength)		// mo or wrong seq array
					|| (itemCount >= listLength)){				// looped throu all items
				jlog.info("NextItem: generating new random sequence:"+itemNo+" ("+itemCount+"/"+dirSize+")");
				seq = new int[listLength];
				for(int i = 0; i < listLength; i++)
					seq[i] = -1;
				
				for(int i = 0; i < listLength; i++){
					int j = (int)(Math.random()*listLength);
					while(seq[j] >= 0 ){
						j = (j+1)%listLength;
					}
					seq[j] = i;
				}
				itemCount = 0;
			}
			if ( itemCount < 0) {
				itemCount += dirSize;
			}
			if( itemCount >= dirSize){
				itemCount -= dirSize;
			}
			itemNo = seq[itemCount];
			jlog.info("NextItem: selected via random seq:"+itemNo+" ("+itemCount+"/"+dirSize+") step="+step);
		}
		else{
			jlog.info("NextItem: stepped to:"+itemNo+" ("+itemCount+"/"+dirSize+") step="+step);
		}

		if ( itemNo < 0) {
			itemNo += dirSize;
		}
		if( itemNo >= dirSize){
			itemNo -= dirSize;
		}

		step = 1;
	

		jlog.info("NextItem: itemNo=:"+itemNo+" ("+itemCount+"/"+dirSize+")");
		if(!isLoop() && itemCount > dirSize){
			jlog.info("NextItem: end of list reached:");
			return -1;
		}
		else
			return itemNo;
	}
}
