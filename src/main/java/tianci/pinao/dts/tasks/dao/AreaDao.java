package tianci.pinao.dts.tasks.dao;

import java.util.List;

import tianci.pinao.dts.models.Channel;
import tianci.pinao.dts.models.Machine;

public interface AreaDao {
	
	// channels
	public List<Channel> getAllChannelsByMachineId(long id);

	// machines
	public List<Machine> getMachineByName(String name);
}
