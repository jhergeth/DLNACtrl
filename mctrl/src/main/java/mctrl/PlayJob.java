package mctrl;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

import org.fourthline.cling.controlpoint.ActionCallback;
import org.fourthline.cling.controlpoint.ControlPoint;
import org.fourthline.cling.model.action.ActionInvocation;
import org.fourthline.cling.model.message.UpnpResponse;
import org.fourthline.cling.model.meta.Service;
import org.fourthline.cling.support.contentdirectory.callback.Browse;
import org.fourthline.cling.support.contentdirectory.callback.Browse.Status;
import org.fourthline.cling.support.model.BrowseFlag;
import org.fourthline.cling.support.model.DIDLContent;
import org.fourthline.cling.support.model.SortCriterion;
import org.fourthline.cling.support.model.item.Item;

public class PlayJob{
	String status;
	String playlist;
	String screen;
	String server;
	String itemTitle;
	String itemPath;
	int item;
	int total;
	int rest;
	int pictTime;
	boolean back;
	boolean forward;
	
	public PlayJob(String status, String playlist, String screen, String server, int item, int total, int rest, int picTime)
	{
		this.status = status; this.playlist = playlist; this.screen = screen; this.server = server; this.total = total; this.rest = rest; this.item = item;
		this.pictTime = picTime;
		back = forward = false;
	}
	public PlayJob(){
		status = "idle";
		playlist = screen = server = "";
		item = total = rest = 0;
		back = forward = false;
	}

	public boolean hasStatus(String s){
		return( status.compareToIgnoreCase(s) == 0);
	}
	
	public void jumpForward(){
		forward = true;
		rest = 0;
	}
	public void jumpBack(){
		back = true;
		rest = 0;
	}
	public void jumpClear(){
		back = forward = false;
	}

	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
//		Main.jlog.log(Level.INFO, "New status of job " + playlist + " on " + screen + ": " + status);
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
	public int getTotal() {
		return total;
	}
	public void setTotal(int total) {
		this.total = total;
	}
	public int getRest() {
		return rest;
	}
	public void setRest(int rest) {
		this.rest = rest;
	}
	public int getItem() {
		return item;
	}
	public void setItem(int item) {
		this.item = item;
	}
	public int getPictTime() {
		return pictTime;
	}
	public void setPictTime(int pictTime) {
		this.pictTime = pictTime;
	}

	public Item getItemFromServer(Service server, ControlPoint cp){
//		Service theSource, final String from, final int no){
	final AtomicReference<Item> res = new AtomicReference<Item>();

	ActionCallback doBrowseAction  = new Browse(server, getPlaylist(), BrowseFlag.DIRECT_CHILDREN, "*", getItem(), new Long(1), (SortCriterion[])null) {
    	
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
	Future<String> f = cp.execute(doBrowseAction);
	
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


}	
