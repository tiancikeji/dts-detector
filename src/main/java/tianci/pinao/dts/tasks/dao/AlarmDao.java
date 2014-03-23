package tianci.pinao.dts.tasks.dao;

import java.util.Date;
import java.util.List;

import tianci.pinao.dts.models.Alarm;

public interface AlarmDao {

	public List<Date> getAlarmDates(Date startDate);

	public void addAlarm(Alarm alarm);

	public void addAlarms(List<Alarm> alarms);

	public void updateAlarms(List<Alarm> alarms);

	public List<Alarm> getAlarms(List<Long> ids, Object[] status);

}
