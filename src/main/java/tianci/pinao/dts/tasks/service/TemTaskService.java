package tianci.pinao.dts.tasks.service;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface TemTaskService {

	public void logTemperature();

	public void readTem();
	
	public void saveTem();

	public Map<Integer, List<String>> getTemsByChannels(Set<Integer> ids);
	
}
