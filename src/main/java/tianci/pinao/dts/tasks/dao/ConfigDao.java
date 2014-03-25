package tianci.pinao.dts.tasks.dao;

import java.util.List;
import java.util.Map;

import tianci.pinao.dts.models.Config;
import tianci.pinao.dts.models.License;

public interface ConfigDao {

	public Map<Integer, Config> getConfigs();

	// license
	public List<License> getAllLicenses();
}
