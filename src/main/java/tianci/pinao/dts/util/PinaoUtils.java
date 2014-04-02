package tianci.pinao.dts.util;

import java.net.InetAddress;
import java.net.NetworkInterface;

public class PinaoUtils {

	public static String LOCAL_MAC_ADDRESS = "unknown";
	
	static{
		try {
			StringBuffer sb = new StringBuffer();
			NetworkInterface ni = NetworkInterface.getByInetAddress(InetAddress.getLocalHost());
			byte[] macs = ni.getHardwareAddress();

			if(macs != null)
				for (int i = 0; i < macs.length; i++) {
					LOCAL_MAC_ADDRESS = Integer.toHexString(macs[i] & 0xFF);
	
					if (LOCAL_MAC_ADDRESS.length() == 1)
						LOCAL_MAC_ADDRESS = '0' + LOCAL_MAC_ADDRESS;
	
					sb.append(LOCAL_MAC_ADDRESS + "-");
	
					LOCAL_MAC_ADDRESS = sb.toString();
					LOCAL_MAC_ADDRESS = LOCAL_MAC_ADDRESS.substring(0, LOCAL_MAC_ADDRESS.length() - 1);
				}

		} catch (Throwable e) {
			InetAddress addr = null;  
	        try {  
	            addr = InetAddress.getLocalHost();  
	        } catch (Throwable t) {  
	            t.printStackTrace();
	        }  
	        if(addr != null){
		        byte[] ipAddr = addr.getAddress();  
		        String ipAddrStr = "";  
		        for (int i = 0; i < ipAddr.length; i++) {  
		            if (i > 0) {  
		                ipAddrStr += ".";  
		            }  
		            ipAddrStr += ipAddr[i] & 0xFF;  
		        }  
		        LOCAL_MAC_ADDRESS = ipAddrStr;
	        }
		}
	}
}
