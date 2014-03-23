package tianci.pinao.dts.tasks;

import org.springframework.context.support.ClassPathXmlApplicationContext;

import tianci.pinao.dts.services.TemService;
import junit.framework.TestCase;

public class TemTaskTest extends TestCase {

	private boolean flag = false;
	
	public void testTem(){
		if(flag){
			TemTask task = new ClassPathXmlApplicationContext("applicationContext.xml").getBean("temTask", TemTask.class);
			
			task.run();
		}
	}
	
	public void testReadTem(){
		if(flag){
			TemService task = getTemService();
			
			task.readTem();
		}
	}
	
	public void testSaveTem(){
		if(flag){
			TemService task = getTemService();
			
			task.saveTem();
		}
	}
	
	public void testCheckTem(){
		if(flag){
			TemService task = getTemService();
			
			task.checkTem();
		}
	}
	
	public void testAlarmTem(){
		if(flag){
			TemService task = getTemService();
			
			task.alarmTem();
		}
	}
	
	public void testCtrlTem(){
		if(flag){
			TemService task = getTemService();
			
			task.ctrlTem();
		}
	}
	
	public void testLogTem(){
		if(flag){
			TemService task = getTemService();
			
			task.logTem();
		}
	}

	private TemService getTemService() {
		TemService task = new ClassPathXmlApplicationContext("applicationContext.xml").getBean("temService", TemService.class);
		return task;
	}
}
