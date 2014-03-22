package tianci.pinao.dts.tasks.dao;

import java.util.List;
import java.util.Map;
import java.util.Set;

import tianci.pinao.dts.models.AreaChannel;
import tianci.pinao.dts.models.AreaHardwareConfig;
import tianci.pinao.dts.models.AreaTempConfig;
import tianci.pinao.dts.models.Channel;
import tianci.pinao.dts.models.Machine;

public interface AreaDao {
	
	// channels
	public List<Channel> getAllChannelsByMachineId(long id);
	
	public List<Channel> getAllChannelsByMachineIds(List<Long> ids);

	// machines
	public List<Machine> getMachineByName(String name);

	// area channels
	public Map<Integer, List<AreaChannel>> getAChannelsByIds(Set<Integer> cIds);

	// area temp configs
	public Map<Integer, AreaTempConfig> getATempsByIds(Set<Integer> aIds);

	// area hard ware configs
	public Map<Integer, AreaHardwareConfig> getAHardsByIds(Set<Integer> aIds);
}
