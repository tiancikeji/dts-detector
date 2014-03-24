package tianci.pinao.dts.models;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

public class AlarmProtocol {
	
	// light states
	public static int LIGHT_STATE_OFF = 0;
	
	public static int LIGHT_STATE_OK = 1;
	
	public static int LIGHT_STATE_FIRE = 2;
	
	public static int LIGHT_STATE_GLITCH = 3;
	
	public static int LIGHT_STATE_GLITCH_FIRE = 4;
	
	// relay states
	public static int RELAY_STATE_ON = 1;
	
	public static int RELAY_STATE_OFF = 0;
	
	// voice states
	public static int VOICE_STATE_FIRE = 1;

	public static int VOICE_STATE_GLITCH = 2;

	public static int VOICE_STATE_GLITCH_FIRE = 1;
	
	public static int VOICE_STATE_OFF = 0;
	
	private int channelId;
	
	private String light;
	
	private int lightState = LIGHT_STATE_OK;
	
	private String relay;
	
	private int relayState = RELAY_STATE_OFF;
	
	private String relay1;
	
	private int relay1State = RELAY_STATE_OFF;
	
	private String voice;
	
	private int voiceState = VOICE_STATE_OFF;

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

	public int getLightState() {
		return lightState;
	}

	public void setLightState(int lightState) {
		this.lightState = lightState;
	}

	public String getRelay() {
		return relay;
	}

	public void setRelay(String relay) {
		this.relay = relay;
	}

	public int getRelayState() {
		return relayState;
	}

	public void setRelayState(int relayState) {
		this.relayState = relayState;
	}

	public String getVoice() {
		return voice;
	}

	public void setVoice(String voice) {
		this.voice = voice;
	}

	public int getVoiceState() {
		return voiceState;
	}

	public void setVoiceState(int voiceState) {
		this.voiceState = voiceState;
	}

	public String getRelay1() {
		return relay1;
	}

	public void setRelay1(String relay1) {
		this.relay1 = relay1;
	}

	public int getRelay1State() {
		return relay1State;
	}

	public void setRelay1State(int relay1State) {
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
