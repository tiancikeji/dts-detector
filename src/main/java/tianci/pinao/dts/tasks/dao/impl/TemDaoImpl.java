package tianci.pinao.dts.tasks.dao.impl;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.support.JdbcDaoSupport;

import tianci.pinao.dts.models.Temperature;
import tianci.pinao.dts.tasks.dao.TemDao;
import tianci.pinao.dts.util.SqlConstants;

public class TemDaoImpl extends JdbcDaoSupport implements TemDao {

	@Override
	public void saveTems(final List<Temperature> tems, final Date date) {
		
		getJdbcTemplate().batchUpdate("insert into " + SqlConstants.TABLE_TEMPERATURE + "(channel, tem, stock, unstock, refer_tem, date, status) values(?, ?, ?, ?, ?, ?, ?)", new BatchPreparedStatementSetter() {
			
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
						ps.setObject(7, tem.getStatus());
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
	public void updateTemsStatus(final List<Temperature> tems, final int statusAlarmed) {
		
		getJdbcTemplate().batchUpdate("update " + SqlConstants.TABLE_TEMPERATURE + " set status = ? where channel = ? and date = ?", new BatchPreparedStatementSetter() {
			
			@Override
			public void setValues(PreparedStatement ps, int index) throws SQLException {
				if(tems.size() > index){
					Temperature tem = tems.get(index);
					if(tem != null){
						ps.setObject(1, statusAlarmed);
						ps.setObject(2, tem.getChannel());
						ps.setObject(3, tem.getDate());
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
	public List<Temperature> getTemsByStatus(List<Integer> cIds, int status) {
		return getJdbcTemplate().query("select channel, tem, stock, unstock, date from " + SqlConstants.TABLE_TEMPERATURE + " where status = ? and channel in (" + StringUtils.join(cIds, ",") + ")", 
				new Object[]{status}, new RowMapper<Temperature>(){

					@Override
					public Temperature mapRow(ResultSet rs, int index) throws SQLException {
						Temperature temp = new Temperature();
						temp.setChannel(rs.getInt("channel"));
						temp.setTem(rs.getString("tem"));
						temp.setStock(rs.getString("stock"));
						temp.setUnstock(rs.getString("unstock"));
						Timestamp ts = rs.getTimestamp("date");
						if(ts != null)
							temp.setDate(new Date(ts.getTime()));
						
						return temp;
					}
				});
	}

	@Override
	public List<Temperature> getTemsByDateNStatus(Date endDate, int status) {
		return getJdbcTemplate().query("select channel, date from " + SqlConstants.TABLE_TEMPERATURE + " where date <= ? and status = ?", 
				new Object[]{endDate, status}, new RowMapper<Temperature>(){
					@Override
					public Temperature mapRow(ResultSet rs, int index) throws SQLException {
						Temperature tem = new Temperature();
						
						tem.setChannel(rs.getInt("channel"));
						Timestamp ts = rs.getTimestamp("date");
						if(ts != null)
							tem.setDate(new Date(ts.getTime()));
						
						return tem;
					}
				});
	}

	@Override
	public void copyTemToTmp(Date endDate, int status) {
		getJdbcTemplate().update("insert into " + SqlConstants.TABLE_TEMPERATURE_TMP + " select channel, tem, stock, unstock, refer_tem, date from " + SqlConstants.TABLE_TEMPERATURE + " where date <= ? and status = ?", new Object[]{endDate, status});
	}

	@Override
	public void removeTemFromTem(Date endDate, int status) {
		getJdbcTemplate().update("delete from " + SqlConstants.TABLE_TEMPERATURE + " where date <= ? and status = ?", new Object[]{endDate, status});
	}

	@Override
	public void copyTemToLog(final List<Temperature> logs) {
		getJdbcTemplate().batchUpdate("insert into " + SqlConstants.TABLE_TEMPERATURE_LOG + " select channel, tem, stock, unstock, refer_tem, date from " + SqlConstants.TABLE_TEMPERATURE + " where channel = ? and date = ?", 
				new BatchPreparedStatementSetter() {
					
					@Override
					public void setValues(PreparedStatement ps, int index) throws SQLException {
						if(logs.size() > index){
							Temperature log = logs.get(index);
							if(log != null){
								ps.setObject(1, log.getChannel());
								ps.setObject(2, log.getDate());
							}
						}
					}
					
					@Override
					public int getBatchSize() {
						return logs.size();
					}
				});
	}

	@Override
	public Map<Integer, Date> getMaxDateFromLog() {
		final Map<Integer, Date> dates = new HashMap<Integer, Date>();
		
		getJdbcTemplate().query("select channel, max(date) as date from " + SqlConstants.TABLE_TEMPERATURE_LOG + " group by channel", new RowMapper<Date>(){
			@Override
			public Date mapRow(ResultSet rs, int index) throws SQLException {
				int channel = rs.getInt("channel");
				Timestamp ts = rs.getTimestamp("date");
				if(ts != null)
					dates.put(channel, new Date(ts.getTime()));
				
				return null;
			}
		});
		
		return dates;
	}

	@Override
	public void removeTemFromTmp(Date endDate) {
		getJdbcTemplate().update("delete from " + SqlConstants.TABLE_TEMPERATURE_TMP + " where date <= ?", new Object[]{endDate});
	}

	@Override
	public void copyTemToEvent(Date start, Date end) {
		getJdbcTemplate().update("insert into " + SqlConstants.TABLE_TEMPERATURE_EVENT + " select channel, tem, stock, unstock, refer_tem, date from " + SqlConstants.TABLE_TEMPERATURE_TMP + " where date between ? and ?", new Object[]{start, end});
	}

	@Override
	public void removeTemFromTmp(Date start, Date end) {
		getJdbcTemplate().update("delete from " + SqlConstants.TABLE_TEMPERATURE_TMP + " where date between ? and ?", new Object[]{start, end});
	}

	@Override
	public List<Temperature> getTemsByIds(int channel, Date date, int limit) {
		return getJdbcTemplate().query("select channel, tem, stock, unstock, date from " + SqlConstants.TABLE_TEMPERATURE + " where channel = ? and date < ? order by date desc limit ?", 
				new Object[]{channel, date, limit}, new RowMapper<Temperature>(){

			@Override
			public Temperature mapRow(ResultSet rs, int index) throws SQLException {
				Temperature temp = new Temperature();
				temp.setChannel(rs.getInt("channel"));
				temp.setTem(rs.getString("tem"));
				temp.setStock(rs.getString("stock"));
				temp.setUnstock(rs.getString("unstock"));
				Timestamp ts = rs.getTimestamp("date");
				if(ts != null)
					temp.setDate(new Date(ts.getTime()));
				
				return temp;
			}
			
		});
	}
}

class TemperatureRowMapper implements RowMapper<Temperature>{

	@Override
	public Temperature mapRow(ResultSet rs, int index) throws SQLException {
		Temperature tem = new Temperature();
		
		tem.setChannel(rs.getInt("channel"));
		tem.setTem(rs.getString("tem"));
		Timestamp ts = rs.getTimestamp("date");
		if(ts != null)
			tem.setDate(new Date(ts.getTime()));
		
		return tem;
	}
}