package tianci.pinao.dts.services.impl;

import tianci.pinao.dts.models.Log;
import tianci.pinao.dts.services.LogService;
import tianci.pinao.dts.tasks.dao.LogDao;

public class LogServiceImpl implements LogService {

	private LogDao logDao;
	
	@Override
	public boolean addLog(Log log) {
		return logDao.addLog(log);
	}

	public LogDao getLogDao() {
		return logDao;
	}

	public void setLogDao(LogDao logDao) {
		this.logDao = logDao;
	}

}
