package tianci.pinao.dts.tasks.dao.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.support.JdbcDaoSupport;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

import tianci.pinao.dts.models.Alarm;
import tianci.pinao.dts.models.Check;
import tianci.pinao.dts.tasks.dao.AlarmDao;
import tianci.pinao.dts.util.SqlConstants;

public class AlarmDaoImpl extends JdbcDaoSupport implements AlarmDao {

	@Override
	public List<Date> getAlarmDates(Date startDate) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void addAlarm(final Alarm alarm) {
		KeyHolder holder = new GeneratedKeyHolder();
		int count = getJdbcTemplate().update(new PreparedStatementCreator() {
			
			@Override
			public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
				PreparedStatement ps = con.prepareStatement("insert into " + SqlConstants.TABLE_ALARM + "(type, machine_id, machine_name, channel_id, channel_name, length, area_id, area_name, alarm_name, light, relay, relay1, voice, temperature, temperature_pre, temperature_max, status, add_time, lastmod_time, lastmod_userid, isdel) values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, now(), now(), ?, 0)", Statement.RETURN_GENERATED_KEYS);
				ps.setObject(1, alarm.getType());
				ps.setObject(2, alarm.getMachineId());
				ps.setObject(3, alarm.getMachineName());
				ps.setObject(4, alarm.getChannelId());
				ps.setObject(5, alarm.getChannelName());
				ps.setObject(6, alarm.getLength());
				ps.setObject(7, alarm.getAreaId());
				ps.setObject(8, alarm.getAreaName());
				ps.setObject(9, alarm.getAlarmName());
				ps.setObject(10, alarm.getLight());
				ps.setObject(11, alarm.getRelay());
				ps.setObject(12, alarm.getRelay1());
				ps.setObject(13, alarm.getVoice());
				ps.setObject(14, alarm.getTemperatureCurr());
				ps.setObject(15, alarm.getTemperaturePre());
				ps.setObject(16, alarm.getTemperatureMax());
				ps.setObject(17, alarm.getStatus());
				ps.setObject(18, alarm.getLastModUserid());
				return ps;
			}
		}, holder);
		
		if(count > 0 && holder.getKey() != null)
			alarm.setId(holder.getKey().longValue());
	}

	@Override
	public void addAlarms(final List<Alarm> alarms) {
		getJdbcTemplate().batchUpdate("insert into " + SqlConstants.TABLE_ALARM + "(type, machine_id, machine_name, channel_id, channel_name, length, area_id, area_name, alarm_name, light, relay, relay1, voice, temperature, temperature_pre, temperature_max, status, add_time, lastmod_time, lastmod_userid, isdel) values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, now(), now(), ?, 0)", 
				new BatchPreparedStatementSetter() {
					
					@Override
					public void setValues(PreparedStatement ps, int i) throws SQLException {
						if(alarms.size() > i){
							Alarm alarm = alarms.get(i);
							ps.setObject(1, alarm.getType());
							ps.setObject(2, alarm.getMachineId());
							ps.setObject(3, alarm.getMachineName());
							ps.setObject(4, alarm.getChannelId());
							ps.setObject(5, alarm.getChannelName());
							ps.setObject(6, alarm.getLength());
							ps.setObject(7, alarm.getAreaId());
							ps.setObject(8, alarm.getAreaName());
							ps.setObject(9, alarm.getAlarmName());
							ps.setObject(10, alarm.getLight());
							ps.setObject(11, alarm.getRelay());
							ps.setObject(12, alarm.getRelay1());
							ps.setObject(13, alarm.getVoice());
							ps.setObject(14, alarm.getTemperatureCurr());
							ps.setObject(15, alarm.getTemperaturePre());
							ps.setObject(16, alarm.getTemperatureMax());
							ps.setObject(17, alarm.getStatus());
							ps.setObject(18, alarm.getLastModUserid());
						}
					}
					
					@Override
					public int getBatchSize() {
						return alarms.size();
					}
				});
	}

	@Override
	public void updateAlarms(final List<Alarm> alarms) {
		getJdbcTemplate().batchUpdate("update " + SqlConstants.TABLE_ALARM + " set status = ? where id = ?", new BatchPreparedStatementSetter() {
			
			@Override
			public void setValues(PreparedStatement ps, int index) throws SQLException {
				if(alarms.size() > index){
					Alarm alarm = alarms.get(index);
					if(alarm != null){
						ps.setObject(1, alarm.getStatus());
						ps.setObject(2, alarm.getId());
					}
				}
			}
			
			@Override
			public int getBatchSize() {
				return alarms.size();
			}
		});
	}

	@Override
	public List<Alarm> getAlarms(List<Long> ids, Object[] status) {
		return getJdbcTemplate().query("select id, type, machine_id, machine_name, channel_id, channel_name, length, area_id, area_name, alarm_name, light, relay, relay1, voice, temperature, temperature_pre, temperature_max, status, add_time, lastmod_time, lastmod_userid, isdel from (select * from " + SqlConstants.TABLE_ALARM + " where isdel = ? and status in (" + StringUtils.join(status, ",") + ") and machine_id in (" + StringUtils.join(ids, ",") + ") order by lastmod_time desc) a group by channel_id, length",  
				new Object[]{0}, new RowMapper<Alarm>(){

					@Override
					public Alarm mapRow(ResultSet rs, int rowNum) throws SQLException {
						Alarm alarm = new Alarm();
						
						alarm.setId(rs.getLong("id"));
						alarm.setType(rs.getInt("type"));
						alarm.setMachineId(rs.getInt("machine_id"));
						alarm.setMachineName(rs.getString("machine_name"));
						alarm.setChannelId(rs.getInt("channel_id"));
						alarm.setChannelName(rs.getString("channel_name"));
						alarm.setAreaId(rs.getInt("area_id"));
						alarm.setAreaName(rs.getString("area_name"));
						alarm.setAlarmName(rs.getString("alarm_name"));
						alarm.setLength(rs.getInt("length"));
						alarm.setLight(rs.getString("light"));
						alarm.setRelay(rs.getString("relay"));
						alarm.setRelay1(rs.getString("relay1"));
						alarm.setVoice(rs.getString("voice"));
						alarm.setTemperatureCurr(rs.getDouble("temperature"));
						alarm.setTemperaturePre(rs.getDouble("temperature_pre"));
						alarm.setTemperatureMax(rs.getDouble("temperature_max"));
						alarm.setStatus(rs.getInt("status"));
						
						return alarm;
					}
			
		});
	}

	@Override
	public boolean hasAlarm(int channel, Date start, Date end) {
		int count = getJdbcTemplate().queryForInt("select count(1) from " + SqlConstants.TABLE_ALARM + " where channel_id = ? and add_time between ? and ?",
				new Object[]{channel, start, end}); 
		return count > 0;
	}

	@Override
	public List<Check> getChecks(int id, int status) {
		return getJdbcTemplate().query("select id, machine_id, channel_id, area_id, light, relay, relay1, voice, status from " + SqlConstants.TABLE_CHECK + " where isdel = ? and status = ? and machine_id = ?", 
				new Object[]{0, status, id}, new RowMapper<Check>() {

			@Override
			public Check mapRow(ResultSet rs, int rowNum) throws SQLException {
				Check check = new Check();
				
				check.setId(rs.getLong("id"));
				check.setMachineId(rs.getInt("machine_id"));
				check.setChannelId(rs.getInt("channel_id"));
				check.setAreaId(rs.getInt("area_id"));
				check.setLight(rs.getString("light"));
				check.setRelay(rs.getString("relay"));
				check.setRelay1(rs.getString("relay1"));
				check.setVoice(rs.getString("voice"));
				check.setStatus(rs.getInt("status"));
				
				return check;
			}
		});
	}

	@Override
	public void updateChecks(final List<Check> checks, final int status) {
		getJdbcTemplate().batchUpdate("update " + SqlConstants.TABLE_CHECK + " set status = ? where id = ?", new BatchPreparedStatementSetter() {
			
			@Override
			public void setValues(PreparedStatement ps, int index) throws SQLException {
				if(checks.size() > index){
					Check check = checks.get(index);
					if(check != null){
						ps.setObject(1, status);
						ps.setObject(2, check.getId());
					}
				}
			}
			
			@Override
			public int getBatchSize() {
				return checks.size();
			}
		});
	}

}
