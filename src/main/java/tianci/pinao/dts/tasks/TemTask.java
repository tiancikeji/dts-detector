package tianci.pinao.dts.tasks;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Service;

import tianci.pinao.dts.services.TemService;

@Service
public class TemTask implements Runnable {

	private final Log logger = LogFactory.getLog(getClass());

	private TemService temService;
	
	private Lock lock = new ReentrantLock();
	
    @Override
    public void run() {
    	try {
    		if(lock.tryLock()){
    			temService.readTem();
    			temService.saveTem();
    			temService.checkTem();
    			temService.alarmTem();
    		}
    	} catch (Throwable e) {
    		if(logger.isErrorEnabled())
    			logger.error("Exception when circle tem >> ", e);
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
