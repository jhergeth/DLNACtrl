package name.hergeth.dlna.api;

import com.fasterxml.jackson.annotation.JsonProperty;

import name.hergeth.dlna.core.DirContent;

import java.util.List;

import org.fourthline.cling.support.model.DIDLObject;
import org.fourthline.cling.support.model.Res;
import org.fourthline.cling.support.model.container.Container;
import org.fourthline.cling.support.model.item.Item;
import org.hibernate.validator.constraints.Length;

public class DirCont {
    private long id;

    public class ITEM {
    	String id;
    	String title;
    	String uri;
    	String tmb;
    	
    	public ITEM(){
    	
    	}
    	
    	public ITEM(String i, String t, String s, String b){
    		id = i;
    		title = t;
    		uri = s;
    		tmb = b;
    	}

        @JsonProperty
		public String getId() {
			return id;
		}

        @JsonProperty
		public String getTitle() {
			return title;
		}

        @JsonProperty
		public String getUri() {
			return uri;
		}
    	
        @JsonProperty
		public String getTmb() {
			return tmb;
		}
    	
    };

    private String dlnaid;
    private String title;
    private ITEM[] items;
    private ITEM[] dirs;

    public DirCont() {
        // Jackson deserialization
    }

    private ITEM[] extractInfo(DIDLObject[] mdl){
    	ITEM[] res = null;
    	
        res = new ITEM[mdl.length];
        for(int i = 0; i < mdl.length; i++){
        	DIDLObject it = mdl[i];
        	if(it != null){
            	List<Res> lr = mdl[i].getResources();
            	String frst = null;
            	String scnd = null;
            	if(lr.size() > 0){
            		scnd = frst = lr.get(0).getValue();
                	if(lr.size() > 1){
                		scnd = lr.get(1).getValue();
                	}
            	}
            	res[i] = new ITEM(it.getId(), it.getTitle() ,frst, scnd);
        	}
        	else{
        		res[i] = null;
        	}
        }
    	
    	return res;
    }
    
    public DirCont(long id, DirContent dc) {
        this.id = id;
        
        this.dlnaid = dc.getId();
        this.title = dc.getTitle();
        
        items = extractInfo(dc.getItems());
        dirs = extractInfo(dc.getDirs());
    }

    @JsonProperty
    public long getId() {
        return id;
    }

    @JsonProperty
    public String getDlnaid() {
		return dlnaid;
	}

    @JsonProperty
    public String getTitle() {
		return title;
	}

    @JsonProperty
    public ITEM[] getItems() {
		return items;
	}

    @JsonProperty
    public ITEM[] getDirs() {
		return dirs;
	}


}