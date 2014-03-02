package tianci.pinao.dts.tasks;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import tianci.pinao.dts.services.TemService;

public class LogTemTask implements Runnable {

	private final Log logger = LogFactory.getLog(getClass());
	
	private TemService temService;
	
	@Override
	public void run() {
		try {
			temService.logTem();
		} catch (Throwable t) {
			if(logger.isErrorEnabled())
				logger.error("Error in logging tem >> ", t);
		}
	}

	public TemService getTemTaskService() {
		return temService;
	}

	public void setTemTaskService(TemService temTaskService) {
		this.temService = temTaskService;
	}
}

