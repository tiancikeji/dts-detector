package tianci.pinao.dts.tasks.dao;

import java.util.Date;
import java.util.List;
import java.util.Map;

import tianci.pinao.dts.models.Temperature;

public interface TemDao {

	public void saveTems(List<Temperature> tems, Date date);

	public void updateTemsStatus(List<Temperature> tems, int statusAlarmed);

	public List<Temperature> getTemsByStatus(List<Integer> cIds, int status);

	public List<Temperature> getTemsByDateNStatus(List<Integer> cIds, Date endDate, int status);

	public void copyTemToTmp(Date endDate, int status);

	public void removeTemFromTem(Date endDate, int status);

	public void copyTemToLog(List<Temperature> logs, boolean stock, boolean refer);

	public Map<Integer, Date> getMaxDateFromLog(List<Integer> cIds);

	public void removeTemFromTmp(Date endDate);

	public void copyTemToEvent(Date start, Date end);

	public void removeTemFromTmp(Date start, Date end);

	public List<Temperature> getTemsByIds(int channel, Date date, int limit);

}
