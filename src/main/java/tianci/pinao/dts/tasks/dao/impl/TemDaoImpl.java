package tianci.pinao.dts.tasks.dao.impl;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;

import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.support.JdbcDaoSupport;

import tianci.pinao.dts.models.Temperature;
import tianci.pinao.dts.tasks.dao.TemDao;
import tianci.pinao.dts.util.SqlConstants;

public class TemDaoImpl extends JdbcDaoSupport implements TemDao {

	@Override
	public void saveTems(final List<Temperature> tems, final Date date) {
		
		getJdbcTemplate().batchUpdate("insert into " + SqlConstants.TABLE_TEMPERATURE + "(channel, tem, stock, unstock, refer_tem, date) values(?, ?, ?, ?, ?, ?)", new BatchPreparedStatementSetter() {
			
			@Override
			public void setValues(PreparedStatement ps, int index) throws SQLException {
				if(tems.size() > index){
					Temperature tem = tems.get(index);
					if(tem != null){
						ps.setObject(1, tem.getChannel());
						ps.setObject(2, tem.getTem());
						ps.setObject(3, tem.getStock());
						ps.setObject(4, tem.getUnstock());
						ps.setObject(5, tem.getReferTem());
						ps.setObject(6, date);
					}
				}
			}
			
			@Override
			public int getBatchSize() {
				return tems.size();
			}
		});
	}

	@Override
	public Timestamp getTemMaxTime() {
		return getJdbcTemplate().queryForObject("select max(`date`) from dts.temperature where `date` < adddate(now(), interval -1 hour)", Timestamp.class);
	}

	@Override
	public void copyTem(Timestamp ts) {
		getJdbcTemplate().update("insert into dts.temperature_log select * from dts.temperature where `date` <= ?", new Object[]{ts});
	}

	@Override
	public void removeTem(Timestamp ts) {
		getJdbcTemplate().update("delete from dts.temperature where `date` <= ?", new Object[]{ts});
	}

}
