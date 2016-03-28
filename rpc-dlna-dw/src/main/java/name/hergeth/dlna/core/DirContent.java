package name.hergeth.dlna.core;

import java.util.List;

import org.fourthline.cling.support.model.container.Container;
import org.fourthline.cling.support.model.item.Item;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class DirContent {

	private String id;
	private String title;
	private List<Item> items;
	private List<Container> dirs;
	
    private Logger jlog = LoggerFactory.getLogger("name.hergeth.dlna.core");

    
	public DirContent(){
		id = null;
		items = null;
		dirs = null;
		title = null;
	}
	public DirContent(String s, String t, List<Item> i, List<Container> d){
		id = s; items = i; dirs = d; title = t;
		jlog.info("DirContent <"+s+">: "+t+" created.");
	}
	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public void setDirs(List<Container> dirs) {
		this.dirs = dirs;
	}

	public void printDirs(){
		for(Container c : dirs){
			jlog.info( "C[" + id + "] " + c.getId() + "=" + c.getTitle());
		}
	}

	public void printItems(){
		for(Item i : items){
			jlog.info("I[" + id + "] " + i.getId() + "=" + i.getTitle());
		}
	}

	public Item[] getItems(){
		Item[] ia = new Item[1];
		return items.toArray(ia);
	}

	public String[] getItemsS(){
		String[] ia = new String[1];
		return items.toArray(ia);
	}
	
	public int getItemSize(){
		return items.size();
	}
	
	public int getDirsSize(){
		return dirs.size();
	}

	public Container[] getDirs(){
		Container[] ca = new Container[1];
		return dirs.toArray(ca);
	}
	public String[] getDirsS(){
		String[] ca = new String[dirs.size()];
		int i = 0;
		for(Container c : dirs){
			ca[i++] = c.getTitle();
		}
		return ca;
	}
}
