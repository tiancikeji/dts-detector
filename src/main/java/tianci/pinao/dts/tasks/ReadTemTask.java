package tianci.pinao.dts.tasks;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Service;

import tianci.pinao.dts.tasks.service.TemTaskService;

@Service
public class ReadTemTask implements Runnable {

	private final Log logger = LogFactory.getLog(getClass());

	private TemTaskService temTaskService;
	
    @Override
    public void run() {
    	try {
    		temTaskService.readTem();
    	} catch (Throwable e) {
    		if(logger.isErrorEnabled())
    			logger.error("Exception when reading term >> ", e);
    	}
    }

	public TemTaskService getTemTaskService() {
		return temTaskService;
	}

	public void setTemTaskService(TemTaskService temTaskService) {
		this.temTaskService = temTaskService;
	}

}
