package tianci.pinao.dts.sal;

public class TemMonitor {

	public native int InitDts(int SwitchCom, int port);
	
	public native int PickData(int ch, int length, double[] Stock, double[] unStock, double[] referTem, double[] Tem, int ret);
	
	public native void CloseDevice();
}
