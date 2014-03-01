package tianci.pinao.dts.tasks.dao;

import java.sql.Timestamp;
import java.util.Date;
import java.util.List;

import tianci.pinao.dts.models.Temperature;

public interface TemDao {

	public void saveTems(List<Temperature> tems, Date date);

	public Timestamp getTemMaxTime();
	
	public void copyTem(Timestamp ts);
	
	public void removeTem(Timestamp ts);

}
