package tianci.pinao.dts.tasks.dao.impl;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.support.JdbcDaoSupport;

import tianci.pinao.dts.models.Config;
import tianci.pinao.dts.models.License;
import tianci.pinao.dts.tasks.dao.ConfigDao;
import tianci.pinao.dts.util.SqlConstants;

public class ConfigDaoImpl extends JdbcDaoSupport implements ConfigDao {

	@Override
	public Map<Integer, Config> getConfigs() {
		final Map<Integer, Config> configs = new HashMap<Integer, Config>();
		getJdbcTemplate().query("select id, type, value, lastmod_time, lastmod_userid from " + SqlConstants.TABLE_CONFIG + " where isdel = ?",  
				new Object[]{0}, new RowMapper<Config>(){

					@Override
					public Config mapRow(ResultSet rs, int index) throws SQLException {
						Config config = new Config();
						
						config.setId(rs.getInt("id"));
						config.setType(rs.getInt("type"));
						config.setValue(rs.getLong("value"));
						Timestamp ts = rs.getTimestamp("lastmod_time");
						if(ts != null)
							config.setLastModTime(new Date(ts.getTime()));
						config.setLastModUserid(rs.getInt("lastmod_userid"));
						
						configs.put(config.getType(), config);
						
						return config;
					}
					
			});
		
		return configs;
	}

	@Override
	public List<License> getAllLicenses() {
		return getJdbcTemplate().query("select id, mac, use_time, lastmod_time, lastmod_userid from " + SqlConstants.TABLE_LICENSE + " where isdel = 0",  new LicenseRowMapper());
	}
}

class LicenseRowMapper implements RowMapper<License>{

	@Override
	public License mapRow(ResultSet rs, int index) throws SQLException {
		License license = new License();

		license.setId(rs.getInt("id"));
		license.setMac(rs.getString("mac"));
		license.setUseTime(rs.getLong("use_time"));
		Timestamp ts = rs.getTimestamp("lastmod_time");
		if(ts != null)
			license.setLastModTime(new Date(ts.getTime()));
		license.setLastModUserid(rs.getInt("lastmod_userid"));
		
		return license;
	}
	
}