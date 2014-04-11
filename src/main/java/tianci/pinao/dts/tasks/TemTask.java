package tianci.pinao.dts.tasks;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Service;

import tianci.pinao.dts.services.ConfigService;
import tianci.pinao.dts.services.TemService;

@Service
public class TemTask implements Runnable {

	private final Log logger = LogFactory.getLog(getClass());

	private TemService temService;
	
	private ConfigService configService;
	
	private Lock lock = new ReentrantLock();
	
    @Override
    public void run() {
    	try {
    		if(lock.tryLock() && configService.checkLifeTime()){
    			temService.readTem();
    			temService.saveTem();
    			temService.checkTem();
    			temService.alarmTem();
    		} else
				logger.info("fail in circle tem, next loop");
    	} catch (Throwable e) {
    		if(logger.isErrorEnabled())
    			logger.error("Exception when circle tem >> ", e);
    	} finally{
			try{
				lock.unlock();
			} catch(Throwable t){
				// left blank;
			}
    	}
    }

	public TemService getTemService() {
		return temService;
	}

	public void setTemService(TemService temService) {
		this.temService = temService;
	}

	public ConfigService getConfigService() {
		return configService;
	}

	public void setConfigService(ConfigService configService) {
		this.configService = configService;
	}

}
