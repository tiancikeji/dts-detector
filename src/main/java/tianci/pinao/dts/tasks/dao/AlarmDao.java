package tianci.pinao.dts.tasks.dao;

import java.util.Date;
import java.util.List;

public interface AlarmDao {

	List<Date> getAlarmDates(Date startDate);

}
