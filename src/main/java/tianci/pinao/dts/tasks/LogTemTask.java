package tianci.pinao.dts.tasks;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import tianci.pinao.dts.tasks.service.TemTaskService;

public class LogTemTask implements Runnable {

	private final Log logger = LogFactory.getLog(getClass());
	
	private TemTaskService temTaskService;
	
	@Override
	public void run() {
		try {
			temTaskService.logTemperature();
		} catch (Throwable t) {
			if(logger.isErrorEnabled())
				logger.error("Error in logTemperature >> ", t);
		}
	}

	public TemTaskService getTemTaskService() {
		return temTaskService;
	}

	public void setTemTaskService(TemTaskService temTaskService) {
		this.temTaskService = temTaskService;
	}
}

