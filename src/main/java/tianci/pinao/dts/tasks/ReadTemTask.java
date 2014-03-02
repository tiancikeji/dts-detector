package tianci.pinao.dts.tasks;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Service;

import tianci.pinao.dts.services.TemService;

@Service
public class ReadTemTask implements Runnable {

	private final Log logger = LogFactory.getLog(getClass());

	private TemService temService;
	
    @Override
    public void run() {
    	try {
    		temService.readTem();
    	} catch (Throwable e) {
    		if(logger.isErrorEnabled())
    			logger.error("Exception when reading tem >> ", e);
    	}
    }

	public TemService getTemService() {
		return temService;
	}

	public void setTemService(TemService temService) {
		this.temService = temService;
	}

}
