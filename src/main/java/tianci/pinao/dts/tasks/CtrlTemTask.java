package tianci.pinao.dts.tasks;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import tianci.pinao.dts.services.TemService;

public class CtrlTemTask implements Runnable {

	private final Log logger = LogFactory.getLog(getClass());
	
	private TemService temService;
	
	private Lock lock = new ReentrantLock();

	@Override
	public void run() {
		try {
			if(lock.tryLock())
    			temService.ctrlTem();
		} catch (Throwable t) {
			if(logger.isErrorEnabled())
				logger.error("Error in ctrling tem >> ", t);
		} finally{
    		lock.unlock();
    	}
	}

	public TemService getTemService() {
		return temService;
	}

	public void setTemService(TemService temService) {
		this.temService = temService;
	}

}
