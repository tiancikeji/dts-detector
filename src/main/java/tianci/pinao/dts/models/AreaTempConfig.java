package tianci.pinao.dts.models;

import java.util.Date;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

public class AreaTempConfig {

	private int id;
	
	private int areaid;
	
	private String areaName;
	
	private int temperatureLow;
	
	private int temperatureHigh;
	
	private int exotherm;
	
	private int temperatureDiff;
	
	private Date lastModTime;
	
	private int lastModUserid;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public int getAreaid() {
		return areaid;
	}

	public void setAreaid(int areaid) {
		this.areaid = areaid;
	}

	public int getTemperatureLow() {
		return temperatureLow;
	}

	public void setTemperatureLow(int temperatureLow) {
		this.temperatureLow = temperatureLow;
	}

	public int getTemperatureHigh() {
		return temperatureHigh;
	}

	public void setTemperatureHigh(int temperatureHigh) {
		this.temperatureHigh = temperatureHigh;
	}

	public int getExotherm() {
		return exotherm;
	}

	public void setExotherm(int exotherm) {
		this.exotherm = exotherm;
	}

	public int getTemperatureDiff() {
		return temperatureDiff;
	}

	public void setTemperatureDiff(int temperatureDiff) {
		this.temperatureDiff = temperatureDiff;
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

	public String getAreaName() {
		return areaName;
	}

	public void setAreaName(String areaName) {
		this.areaName = areaName;
	}
}
