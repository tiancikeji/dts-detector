package tianci.pinao.dts.util;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;

public class PinaoUtils {

	public static String LOCAL_MAC_ADDRESS;
	
	static{
		try {
			StringBuffer sb = new StringBuffer();
			NetworkInterface ni = NetworkInterface.getByInetAddress(InetAddress.getLocalHost());
			byte[] macs = ni.getHardwareAddress();

			for (int i = 0; i < macs.length; i++) {
				LOCAL_MAC_ADDRESS = Integer.toHexString(macs[i] & 0xFF);

				if (LOCAL_MAC_ADDRESS.length() == 1)
					LOCAL_MAC_ADDRESS = '0' + LOCAL_MAC_ADDRESS;

				sb.append(LOCAL_MAC_ADDRESS + "-");

				LOCAL_MAC_ADDRESS = sb.toString();
				LOCAL_MAC_ADDRESS = LOCAL_MAC_ADDRESS.substring(0, LOCAL_MAC_ADDRESS.length() - 1);
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
