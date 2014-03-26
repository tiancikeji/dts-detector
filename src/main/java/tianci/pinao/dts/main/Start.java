package tianci.pinao.dts.main;

import org.springframework.context.support.ClassPathXmlApplicationContext;

public class Start {

	public static void main(String[] args) {
		new ClassPathXmlApplicationContext("classpath:applicationContext.xml").getBean("scheduler");
	}
}
