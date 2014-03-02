package tianci.pinao.dts.tasks;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import tianci.pinao.dts.services.TemService;

public class SaveTemTask implements Runnable {

	private final Log logger = LogFactory.getLog(getClass());

	private TemService temService;
	
    @Override
    public void run() {
    	try{
    		temService.saveTem();
    	} catch(Throwable t){
    		if(logger.isErrorEnabled())
    			logger.error("Exception when saving term >> ", t);
    	}
    }

	public TemService getTemService() {
		return temService;
	}

	public void setTemService(TemService temService) {
		this.temService = temService;
	}

}

