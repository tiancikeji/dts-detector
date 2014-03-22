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
			TemService task = new ClassPathXmlApplicationContext("applicationContext.xml").getBean("temService", TemService.class);
			
			task.readTem();
		}
	}
	
	public void testSaveTem(){
		if(flag){
			TemService task = new ClassPathXmlApplicationContext("applicationContext.xml").getBean("temService", TemService.class);
			
			task.saveTem();
		}
	}
	
	public void testCheckTem(){
		if(flag){
			TemService task = new ClassPathXmlApplicationContext("applicationContext.xml").getBean("temService", TemService.class);
			
			task.checkTem();
		}
	}
	
	public void testAlarmTem(){
		if(flag){
			TemService task = new ClassPathXmlApplicationContext("applicationContext.xml").getBean("temService", TemService.class);
			
			task.alarmTem();
		}
	}
	
	public void testCtrlTem(){
		if(flag){
			TemService task = new ClassPathXmlApplicationContext("applicationContext.xml").getBean("temService", TemService.class);
			
			task.ctrlTem();
		}
	}
}
