package tianci.pinao.dts.models;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

public class AlarmProtocol {
	
	// light states
	public static String LIGHT_STATE_OFF = "00";
	
	public static String LIGHT_STATE_NORMAL = "01";
	
	public static String LIGHT_STATE_FIRE = "02";
	
	public static String LIGHT_STATE_GLITCH = "03";
	
	public static String LIGHT_STATE_GLITCH_FIRE = "04";
	
	public static String LIGHT_STATE_ALL = "05";
	
	public static String LIGHT_STATE_RESET = "06";
	
	// relay states
	public static String RELAY_STATE_ON = "01";
	
	public static String RELAY_STATE_OFF = "00";
	
	// voice states
	public static String VOICE_STATE_ON = "01";
	
	public static String VOICE_STATE_OFF = "00";
	
	// header
	public static String HEADER = "<<";

	public static String TAILER = ">>";
	
	// extension address
	public static String ADDRESS_EXTENSION = "1";
	
	// reset
	public static String ADDRESS_RESET = "99";
	
	public static String RESET_STATE_OFF = "00";
	
	public static String RESET_STATE_ON = "01";
	
	// GPIO
	public static String ADDRESS_GPIO = "A1";
	
	public static String GPIO_STATE_OFF = "00";
	
	public static String GPIO_STATE_FIRE = "01";
	
	public static String GPIO_STATE_GLITCH = "02";
	
	public static String GPIO_STATE_ON = "04";
	
	private int channelId;
	
	private String light;
	
	private String lightState = LIGHT_STATE_NORMAL;
	
	private String relay;
	
	private String relayState = RELAY_STATE_OFF;
	
	private String voice;
	
	private String voiceState = VOICE_STATE_OFF;
	
	private String relay1;
	
	private String relay1State = RELAY_STATE_OFF;

	public int getChannelId() {
		return channelId;
	}

	public void setChannelId(int channelId) {
		this.channelId = channelId;
	}

	public String getLight() {
		return light;
	}

	public void setLight(String light) {
		this.light = light;
	}

	public String getLightState() {
		return lightState;
	}

	public void setLightState(String lightState) {
		this.lightState = lightState;
	}

	public String getRelay() {
		return relay;
	}

	public void setRelay(String relay) {
		this.relay = relay;
	}

	public String getRelayState() {
		return relayState;
	}

	public void setRelayState(String relayState) {
		this.relayState = relayState;
	}

	public String getVoice() {
		return voice;
	}

	public void setVoice(String voice) {
		this.voice = voice;
	}

	public String getVoiceState() {
		return voiceState;
	}

	public void setVoiceState(String voiceState) {
		this.voiceState = voiceState;
	}

	public String getRelay1() {
		return relay1;
	}

	public void setRelay1(String relay1) {
		this.relay1 = relay1;
	}

	public String getRelay1State() {
		return relay1State;
	}

	public void setRelay1State(String relay1State) {
		this.relay1State = relay1State;
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

}
