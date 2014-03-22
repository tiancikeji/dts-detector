package tianci.pinao.dts.tasks.dao.impl;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.support.JdbcDaoSupport;

import tianci.pinao.dts.models.AreaChannel;
import tianci.pinao.dts.models.AreaHardwareConfig;
import tianci.pinao.dts.models.AreaTempConfig;
import tianci.pinao.dts.models.Channel;
import tianci.pinao.dts.models.Machine;
import tianci.pinao.dts.tasks.dao.AreaDao;
import tianci.pinao.dts.util.SqlConstants;

public class AreaDaoImpl extends JdbcDaoSupport implements AreaDao {

	@Override
	public List<Channel> getAllChannelsByMachineId(long id) {
		return getJdbcTemplate().query("select id, machine_id, name, length, lastmod_time, lastmod_userid from " + SqlConstants.TABLE_CHANNEL + " where machine_id = ? and isdel = ?",  
				new Object[]{id, 0}, new ChannelRowMapper());
	}

	@Override
	public List<Channel> getAllChannelsByMachineIds(List<Long> ids) {
		return getJdbcTemplate().query("select id, machine_id, name, length, lastmod_time, lastmod_userid from " + SqlConstants.TABLE_CHANNEL + " where isdel = ? and machine_id in (" + StringUtils.join(ids, ",") + ")",  
				new Object[]{0}, new ChannelRowMapper());
	}

	@Override
	public List<Machine> getMachineByName(String name) {
		return getJdbcTemplate().query("select id, name, serial_port, baud_rate, lastmod_time, lastmod_userid from " + SqlConstants.TABLE_MACHINE + " where name = ? and isdel = ?", 
				new Object[]{name, 0}, new MachineRowMapper());
	}

	@Override
	public Map<Integer, List<AreaChannel>> getAChannelsByIds(Set<Integer> cIds) {
		final Map<Integer, List<AreaChannel>> result = new HashMap<Integer, List<AreaChannel>>();
		
		getJdbcTemplate().query("select ac.id as id, ac.name as name, ac.area_id as area_id, a.name as area_name, ac.channel_id as channel_id, c.name as channel_name, c.machine_id as machine_id, m.name as machine_name, ac.start as start, ac.end as end from " + SqlConstants.TABLE_AREA_CHANNEL + "ac, " + SqlConstants.TABLE_AREA + " a, " + SqlConstants.TABLE_CHANNEL + " c, " + SqlConstants.TABLE_MACHINE + " m where m.isdel = 0 and m.id = c.machine_id and c.isdel = 0 and c.id = ac.channel_id and a.isdel = 0 and a.id = ac.area_id and ac.isdel = 0 and ac.channel_id in (" + StringUtils.join(cIds, ",") + ")",
				new RowMapper<Integer>(){

					@Override
					public Integer mapRow(ResultSet rs, int index) throws SQLException {
						AreaChannel ac = new AreaChannel();
						ac.setId(rs.getInt("id"));
						ac.setName(rs.getString("name"));
						ac.setAreaid(rs.getInt("area_id"));
						ac.setAreaName(rs.getString("area_name"));
						ac.setChannelid(rs.getInt("channel_id"));
						ac.setChannelName(rs.getString("channel_name"));
						ac.setMachineid(rs.getInt("machine_id"));
						ac.setMachineName(rs.getString("machine_name"));
						ac.setStart(rs.getInt("start"));
						ac.setEnd(rs.getInt("end"));
						
						List<AreaChannel> tmp = result.get(ac.getChannelid());
						if(tmp == null){
							tmp = new ArrayList<AreaChannel>();
							result.put(ac.getChannelid(), tmp);
						}
						tmp.add(ac);
						
						return index;
					}
			
		});
		
		return result;
	}

	@Override
	public Map<Integer, AreaTempConfig> getATempsByIds(Set<Integer> aIds) {
		final Map<Integer, AreaTempConfig> result = new HashMap<Integer, AreaTempConfig>();
		
		getJdbcTemplate().query("select id, area_id, temperature_low, temperature_high, exotherm, temperature_diff from " + SqlConstants.TABLE_AREA_TEMP_CONFIG + " where isdel = 0", new RowMapper<Integer>(){

			@Override
			public Integer mapRow(ResultSet rs, int index) throws SQLException {
				AreaTempConfig temp = new AreaTempConfig();
				temp.setId(rs.getInt("id"));
				temp.setAreaid(rs.getInt("area_id"));
				temp.setTemperatureLow(rs.getInt("temperature_low"));
				temp.setTemperatureHigh(rs.getInt("temperature_high"));
				temp.setTemperatureDiff(rs.getInt("temperature_diff"));
				temp.setExotherm(rs.getInt("exotherm"));
				
				result.put(temp.getAreaid(), temp);
				
				return index;
			}
			
		});
		
		return result;
	}

	@Override
	public Map<Integer, AreaHardwareConfig> getAHardsByIds(Set<Integer> aIds) {
		final Map<Integer, AreaHardwareConfig> result = new HashMap<Integer, AreaHardwareConfig>();
		
		getJdbcTemplate().query("select id, area_id, relay1, light, relay, voice from " + SqlConstants.TABLE_AREA_HARDWARE_CONFIG + " where isdel = 0", new RowMapper<Integer>(){

			@Override
			public Integer mapRow(ResultSet rs, int index) throws SQLException {
				AreaHardwareConfig temp = new AreaHardwareConfig();
				temp.setId(rs.getInt("id"));
				temp.setAreaid(rs.getInt("area_id"));
				temp.setRelay1(rs.getString("relay1"));
				temp.setLight(rs.getString("light"));
				temp.setRelay(rs.getString("relay"));
				temp.setVoice(rs.getString("voice"));
				
				result.put(temp.getAreaid(), temp);
			
				return index;
			}
			
		});
		
		return result;
	}
}

class MachineRowMapper implements RowMapper<Machine>{

	@Override
	public Machine mapRow(ResultSet rs, int index) throws SQLException {
		Machine machine = new Machine();
		machine.setId(rs.getInt("id"));
		machine.setName(rs.getString("name"));
		machine.setSerialPort(rs.getString("serial_port"));
		machine.setBaudRate(rs.getString("baud_rate"));
		Timestamp ts = rs.getTimestamp("lastmod_time");
		if(ts != null)
			machine.setLastModTime(new Date(ts.getTime()));
		machine.setLastModUserid(rs.getInt("lastmod_userid"));
		return machine;
	}
	
}

class ChannelRowMapper implements RowMapper<Channel>{

	@Override
	public Channel mapRow(ResultSet rs, int index) throws SQLException {
		Channel channel = new Channel();
		channel.setId(rs.getInt("id"));
		channel.setMachineid(rs.getInt("machine_id"));
		channel.setName(rs.getString("name"));
		channel.setLength(rs.getInt("length"));
		Timestamp ts = rs.getTimestamp("lastmod_time");
		if(ts != null)
			channel.setLastModTime(new Date(ts.getTime()));
		channel.setLastModUserid(rs.getInt("lastmod_userid"));
		return channel;
	}
	
}