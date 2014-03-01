package tianci.pinao.dts.tasks.dao.impl;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.support.JdbcDaoSupport;

import tianci.pinao.dts.models.Channel;
import tianci.pinao.dts.models.Machine;
import tianci.pinao.dts.tasks.dao.AreaDao;
import tianci.pinao.dts.util.SqlConstants;

public class AreaDaoImpl extends JdbcDaoSupport implements AreaDao {

	@Override
	public List<Channel> getAllChannelsByMachineId(long id) {
		return getJdbcTemplate().query("select id, machine_id, name, length, lastmod_time, lastmod_userid from " + SqlConstants.TABLE_CHANNEL + " where machine_id = ? and isel = ?",  
				new Object[]{id, 0}, new ChannelRowMapper());
	}

	@Override
	public List<Machine> getMachineByName(String name) {
		return getJdbcTemplate().query("select id, name, serial_port, baud_rate, lastmod_time, lastmod_userid from " + SqlConstants.TABLE_MACHINE + " where name = ? and isdel = ?", 
				new Object[]{name, 0}, new MachineRowMapper());
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