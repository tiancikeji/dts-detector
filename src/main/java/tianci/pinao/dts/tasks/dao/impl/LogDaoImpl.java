package tianci.pinao.dts.tasks.dao.impl;

import org.springframework.jdbc.core.support.JdbcDaoSupport;

import tianci.pinao.dts.models.Log;
import tianci.pinao.dts.tasks.dao.LogDao;
import tianci.pinao.dts.util.SqlConstants;

public class LogDaoImpl extends JdbcDaoSupport implements LogDao {

	@Override
	public boolean addLog(Log log) {
		int count = getJdbcTemplate().update("insert into " + SqlConstants.TABLE_LOG + "(type, value, source, lastmod_time, isdel) values(?, ?, ?,now(),?)", 
				new Object[]{log.getType(), log.getValue(), log.getSource(), 0});
		return count > 0;
	}
}
