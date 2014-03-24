package tianci.pinao.dts.models;

import java.util.Date;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

public class Alarm {
	
	// types
	public static final int TYPE_TEMPERATURE_LOW = 1;
	
	public static final int TYPE_TEMPERATURE_HIGH = 2;
	
	public static final int TYPE_TEMPERATURE_DIFF = 3;
	
	public static final int TYPE_TEMPERATURE_EXOTHERM = 4;
	
	public static final int TYPE_TEMPERATURE_EXTREME_LOW = 5;
	
	public static final int TYPE_TEMPERATURE_EXTREME_HIGH = 6;
	
	public static final int TYPE_STOCK_LOW = 7;
	
	public static final int TYPE_UNSTOCK_LOW = 8;
	
	public static final int TYPE_FREE_SPACE = 9;
	
	// status
	public static final int STATUS_NEW = 0;
	
	public static final int STATUS_ALARMED = 1;
	
	public static final int STATUS_NOTIFY = 2;
	
	public static final int STATUS_MUTE = 3;
	
	public static final int STATUS_MUTED = 4;
	
	public static final int STATUS_RESET = 5;
	
	public static final int STATUS_RESETED = 6;
	
	private long id;
	
	private int type;
	
	private int machineId;
	
	private String machineName;
	
	private int channelId;
	
	private String channelName;
	
	private int length;
	
	private int areaId;
	
	private String areaName;
	
	private String alarmName;
	
	private String light;
	
	private String relay;
	
	private String relay1;
	
	private String voice;
	
	private double temperatureCurr;
	
	private double temperaturePre;
	
	private double temperatureMax;
	
	private int status;
	
	private Date addTime;
	
	private int lastModUserid;
	
	private Date lastModTime;

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

	public int getMachineId() {
		return machineId;
	}

	public void setMachineId(int machineId) {
		this.machineId = machineId;
	}

	public String getMachineName() {
		return machineName;
	}

	public void setMachineName(String machineName) {
		this.machineName = machineName;
	}

	public int getChannelId() {
		return channelId;
	}

	public void setChannelId(int channelId) {
		this.channelId = channelId;
	}

	public String getChannelName() {
		return channelName;
	}

	public void setChannelName(String channelName) {
		this.channelName = channelName;
	}

	public int getLength() {
		return length;
	}

	public void setLength(int length) {
		this.length = length;
	}

	public int getAreaId() {
		return areaId;
	}

	public void setAreaId(int areaId) {
		this.areaId = areaId;
	}

	public String getAreaName() {
		return areaName;
	}

	public void setAreaName(String areaName) {
		this.areaName = areaName;
	}

	public String getLight() {
		return light;
	}

	public void setLight(String light) {
		this.light = light;
	}

	public String getRelay() {
		return relay;
	}

	public void setRelay(String relay) {
		this.relay = relay;
	}

	public String getVoice() {
		return voice;
	}

	public void setVoice(String voice) {
		this.voice = voice;
	}

	public double getTemperatureCurr() {
		return temperatureCurr;
	}

	public void setTemperatureCurr(double temperatureCurr) {
		this.temperatureCurr = temperatureCurr;
	}

	public double getTemperaturePre() {
		return temperaturePre;
	}

	public void setTemperaturePre(double temperaturePre) {
		this.temperaturePre = temperaturePre;
	}

	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
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

	public String getAlarmName() {
		return alarmName;
	}

	public void setAlarmName(String alarmName) {
		this.alarmName = alarmName;
	}

	public int getLastModUserid() {
		return lastModUserid;
	}

	public void setLastModUserid(int lastModUserid) {
		this.lastModUserid = lastModUserid;
	}

	public Date getLastModTime() {
		return lastModTime;
	}

	public void setLastModTime(Date lastModTime) {
		this.lastModTime = lastModTime;
	}

	public String getRelay1() {
		return relay1;
	}

	public void setRelay1(String relay1) {
		this.relay1 = relay1;
	}

	public Date getAddTime() {
		return addTime;
	}

	public void setAddTime(Date addTime) {
		this.addTime = addTime;
	}

	public double getTemperatureMax() {
		return temperatureMax;
	}

	public void setTemperatureMax(double temperatureMax) {
		this.temperatureMax = temperatureMax;
	}
}
