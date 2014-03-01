package tianci.pinao.dts.tasks.dao;

import java.util.Map;

import tianci.pinao.dts.models.Config;

public interface ConfigDao {

	public Map<Integer, Config> getConfigs();
}
