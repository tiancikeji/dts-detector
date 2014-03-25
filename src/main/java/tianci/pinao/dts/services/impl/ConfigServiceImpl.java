package tianci.pinao.dts.services.impl;

import java.util.List;

import tianci.pinao.dts.models.Config;
import tianci.pinao.dts.models.License;
import tianci.pinao.dts.services.ConfigService;
import tianci.pinao.dts.tasks.dao.ConfigDao;

public class ConfigServiceImpl implements ConfigService{
	
	private ConfigDao configDao;

	@Override
	public boolean checkLifeTime() {
		Config config = getLicenseConfig();
		if(config != null){
			List<License> licenses = getAllLicenses();
			if(licenses != null && licenses.size() > 0)
				for(License license : licenses)
					if(license != null && license.getUseTime() >= config.getValue())
						return false;
			return true;
		} else
			return false;
	}

	private Config getLicenseConfig() {
		return configDao.getConfigs().get(Config.TYPE_LIFE_TIME_FLAG);
	}

	private List<License> getAllLicenses() {
		return configDao.getAllLicenses();
	}

	public ConfigDao getConfigDao() {
		return configDao;
	}

	public void setConfigDao(ConfigDao configDao) {
		this.configDao = configDao;
	}

}
