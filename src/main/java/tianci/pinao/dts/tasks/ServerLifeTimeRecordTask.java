package tianci.pinao.dts.tasks;

import java.net.InetAddress;
import java.net.UnknownHostException;

import tianci.pinao.dts.models.Log;
import tianci.pinao.dts.services.LogService;
import tianci.pinao.dts.util.PinaoUtils;

public class ServerLifeTimeRecordTask {
	
	private LogService logService;
	
	private Log log;
	
	public ServerLifeTimeRecordTask(){
		log = new Log();
		try {
			log.setValue(InetAddress.getLocalHost().getHostAddress().toString());
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		log.setSource(PinaoUtils.LOCAL_MAC_ADDRESS);
	}
	
	public void init(){
		log.setType(Log.TYPE_DETECT_SERVER_START);
		logService.addLog(log);
	}
	
	public void destroy(){
		log.setType(Log.TYPE_DETECT_SERVER_STOP);
		logService.addLog(log);
	}

	public LogService getLogService() {
		return logService;
	}

	public void setLogService(LogService logService) {
		this.logService = logService;
	}

}
