package tianci.pinao.dts.tasks.dao;

import java.util.Date;
import java.util.List;

import tianci.pinao.dts.models.Alarm;
import tianci.pinao.dts.models.Check;

public interface AlarmDao {

	public List<Date> getAlarmDates(Date startDate);

	public void addAlarm(Alarm alarm);

	public void addAlarms(List<Alarm> alarms);

	public void updateAlarms(List<Alarm> alarms);

	public List<Alarm> getAlarms(List<Long> ids, Object[] status);

	public boolean hasAlarm(int channel, Date start, Date end);

	public List<Check> getChecks(int id, int status);

	public void updateChecks(List<Check> checks, int status);

}
