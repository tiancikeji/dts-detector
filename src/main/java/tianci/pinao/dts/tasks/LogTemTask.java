package tianci.pinao.dts.tasks;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import tianci.pinao.dts.services.ConfigService;
import tianci.pinao.dts.services.TemService;

public class LogTemTask implements Runnable {

	private final Log logger = LogFactory.getLog(getClass());
	
	private TemService temService;
	
	private ConfigService configService;
	
	private Lock lock = new ReentrantLock();
	
	@Override
	public void run() {
		try {
			if(lock.tryLock() && configService.checkLifeTime())
    			temService.logTem();
			else
				logger.info("fail in log tem, next loop");
		} catch (Throwable t) {
			if(logger.isErrorEnabled())
				logger.error("Error in logging tem >> ", t);
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

