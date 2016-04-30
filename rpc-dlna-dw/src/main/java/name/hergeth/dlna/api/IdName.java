package name.hergeth.dlna.api;

import com.fasterxml.jackson.annotation.JsonProperty;

public class IdName {
	private long id;

	public class IDNAME {
		public String id;
		public String name;

		public IDNAME(String i, String n) {
			id = i;
			name = n;
		}

	};

	private IDNAME result[];

	public IdName() {
		// Jackson deserialization
	}

	public IdName(long id, String[] ids, String[] names) {
		this.id = id;
		this.result = new IDNAME[ids.length];
		for (int i = 0; i < ids.length; i++) {
			if (i >= names.length)
				break;
			this.result[i] = new IDNAME(ids[i], names[i]);
		}
	}

	@JsonProperty
	public long getId() {
		return id;
	}

	@JsonProperty
	public IDNAME[] getContent() {
		return result;
	}
}