package tianci.pinao.dts.models;

import java.util.Date;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

public class Config {
	
	public static final int TYPE_LIFE_TIME_FLAG = 1;
	
	public static final int TYPE_STOCK_FLAG = 2;
	
	public static final int TYPE_REFER_TEM_FLAG = 3;
	
	public static final int TYPE_BACK_INTERVAL_FLAG = 4;
	
	public static final int TYPE_REFRESH_INTERVAL_FLAG = 5;
	
	public static final int VALUE_SAVE = 1;

	private int id;
	
	private int type;
	
	private long value;
	
	private long used;
	
	private Date lastModTime;
	
	private int lastModUserid;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

	public long getValue() {
		return value;
	}

	public void setValue(long value) {
		this.value = value;
	}

	public Date getLastModTime() {
		return lastModTime;
	}

	public void setLastModTime(Date lastModTime) {
		this.lastModTime = lastModTime;
	}

	public int getLastModUserid() {
		return lastModUserid;
	}

	public void setLastModUserid(int lastModUserid) {
		this.lastModUserid = lastModUserid;
	}

	@Override
	public int hashCode() {
		return HashCodeBuilder.reflectionHashCode(this);
	}

	@Override
	public boolean equals(Object obj) {
		return EqualsBuilder.reflectionEquals(this, obj);
	}

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this);
	}

	public long getUsed() {
		return used;
	}

	public void setUsed(long used) {
		this.used = used;
	}
}
