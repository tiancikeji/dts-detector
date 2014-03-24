package tianci.pinao.dts.sal;

public class TemAlarm {

	public native boolean InitPort(int port, int baud);
	
	// deviceid : 0
	public native boolean SendData(int deviceId, int[] states, int length);
	
	public native void ClosePort();
}
