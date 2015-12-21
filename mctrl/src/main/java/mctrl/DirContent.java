package mctrl;

import java.util.List;
import java.util.logging.Level;

import org.fourthline.cling.support.model.container.Container;
import org.fourthline.cling.support.model.item.Item;

public class DirContent {
	String id;
	String title;
	List<Item> items;
	List<Container> dirs;

	public DirContent(){
		id = null;
		items = null;
		dirs = null;
		title = null;
	}
	public DirContent(String s, String t, List<Item> i, List<Container> d){
		id = s; items = i; dirs = d; title = t;
		Main.jlog.log(Level.INFO,  "DirContent <"+s+">: "+t+" created.");
	}

	public void printDirs(){
		for(Container c : dirs){
			Main.jlog.log(Level.INFO, "C[" + id + "] " + c.getId() + "=" + c.getTitle());
		}
	}

	public void printItems(){
		for(Item i : items){
			Main.jlog.log(Level.INFO, "I[" + id + "] " + i.getId() + "=" + i.getTitle());
		}
	}

	public Item[] getItems(){
		Item[] ia = new Item[1];
		return items.toArray(ia);
	}

	public Container[] getDirs(){
		Container[] ca = new Container[1];
		return dirs.toArray(ca);
	}
}
