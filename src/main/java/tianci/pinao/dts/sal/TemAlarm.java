package tianci.pinao.dts.sal;

public class TemAlarm {

	public native int InitRelay(int SwitchCom, int port);
	
	public native int ConCtrolRelay(int[] state);
	
	public native void CloseRelay();
}
