package tianci.pinao.dts.services.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import tianci.pinao.dts.models.Channel;
import tianci.pinao.dts.models.Config;
import tianci.pinao.dts.models.Machine;
import tianci.pinao.dts.models.Temperature;
import tianci.pinao.dts.sal.TemMonitor;
import tianci.pinao.dts.services.TemService;
import tianci.pinao.dts.tasks.dao.AreaDao;
import tianci.pinao.dts.tasks.dao.ConfigDao;
import tianci.pinao.dts.tasks.dao.TemDao;
import tianci.pinao.dts.util.PinaoConstants;

public class TemServiceImpl implements TemService {
	
	private Log logger = LogFactory.getLog(getClass());

	private String machineId;

	private String path = PinaoConstants.TEM_PATH;
	
	private File dir = new File(path);
	
	private AreaDao areaDao;
	
	private ConfigDao configDao;
	
	private TemDao temDao;
	
	public TemServiceImpl(){
		// load DLL
		loadDLL();
		
    	if(!(dir.exists() && dir.isDirectory()))
    		dir.mkdirs();
	}

	@Override
	public void saveTem() {
		long start = System.currentTimeMillis();
		try{
	    	// read files
    		File[] files = getToSaveFiles();
    		
    		if(files != null && files.length > 0)
    			for(File file : files)
    				if(file.exists()){
    					long tmp = System.currentTimeMillis();
    					boolean result = false;
    					try{
    						saveFileTmpData(file);
    						result = true;
    					} catch(Throwable t){
							if(logger.isErrorEnabled())
								logger.error("Exception in saveFileTmpData for path <" + file + ">", t);
    					} finally{
							if(logger.isInfoEnabled())
								logger.info("End read saveFileTmpData for <" + file.getName() + "> used <" + (System.currentTimeMillis() - tmp) + "> result <" + result + ">");
						}
    				}
		} finally {
	    	if(logger.isInfoEnabled())
	    		logger.info("SaveTemTask used <" + (System.currentTimeMillis() - start) + ">");	
		}
	}

	private void saveFileTmpData(File file) throws Throwable {
		File path = new File(file.getAbsolutePath() + PinaoConstants.TEM_WRITE_SUFFIX);
		file.renameTo(path);
		
		try{
			// read file-data
			List<Temperature> tems = readTemDataFromFile(path);
			
			Date date = null;
			long tmp = NumberUtils.toLong(StringUtils.removeEnd(path.getName(), PinaoConstants.TEM_WRITE_SUFFIX), -1);
			if(tmp <= 0)
				date = new Date();
			else
				date = new Date(tmp);
			
			// save into db
			if(tems != null && tems.size() > 0)
				temDao.saveTems(tems, date);
		} catch(Throwable t){
			// rename back...
			path.renameTo(file);
			throw t;
		}
		
		// delete files
		path.delete();
		
		// TODO check alarm logic
		
		// TODO alarm...
	}

	public List<Temperature> readTemDataFromFile(File path) throws FileNotFoundException {
		List<Temperature> data = new ArrayList<Temperature>();
		
		Scanner sc = new Scanner(new FileInputStream(path));
		while(sc.hasNextLine()){
			String line = sc.nextLine();
			if(StringUtils.isNotBlank(line)){
				String[] cols = StringUtils.split(line, PinaoConstants.TEM_DATA_COL_SEP);
				if(cols != null && cols.length > 1){
					Temperature tem = new Temperature();
					tem.setChannel(NumberUtils.toInt(cols[0]));
					tem.setTem(cols[1]);
					if(cols.length > 3)
						tem.setStock(cols[2]);
					if(cols.length > 4)
						tem.setUnstock(cols[3]);
					if(cols.length > 5)
						tem.setReferTem(NumberUtils.toDouble(cols[4]));
					data.add(tem);
				}
			}
		}
		sc.close();
		
		return data;
	}

	private File[] getToSaveFiles() {
		File[] files = dir.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return NumberUtils.isNumber(name);
			}
		});
		return files;
	}

	@Override
	public void readTem() {
		if(logger.isInfoEnabled())
			logger.info("Starting read tem for <" + machineId + ">");
		long start = System.currentTimeMillis();
		
		try{
			if(StringUtils.isNotBlank(machineId)){
				// get machine configs
				List<Machine> machines = areaDao.getMachineByName(machineId);
				if(machines != null && machines.size() > 0){
					Map<Integer, Config> configs = configDao.getConfigs();
					for(Machine machine : machines){
						long tmp = System.currentTimeMillis();
						boolean result = false;
						try{
							readMachineTem(machine, configs);
							result = true;
						} catch(Throwable t) {
							if(logger.isErrorEnabled())
								logger.error("Exception in readMachineTem for machine <" + machine + ">", t);
						} finally{
							if(logger.isInfoEnabled())
								logger.info("End read readMachineTem for <" + machine.getId() + "> used <" + (System.currentTimeMillis() - tmp) + "> result <" + result + ">");
						}
					}
				}
			}
		} finally{
			if(logger.isInfoEnabled())
				logger.info("End read tem for <" + machineId + "> used <" + (System.currentTimeMillis() - start) + ">");
		}
	}

	private void readMachineTem(Machine machine, Map<Integer, Config> configs) {
		// open device
		TemMonitor tm = newTemMonitor(NumberUtils.toInt(machine.getSerialPort(), -1), NumberUtils.toInt(machine.getBaudRate(), -1));
		
		// get channels
		List<Channel> channels = areaDao.getAllChannelsByMachineId(machine.getId());
		
		if(channels != null && channels.size() > 0)
			for(Channel channel : channels){
				long tmp = System.currentTimeMillis();
				boolean result = false;
				try{
			    	readChannelTem(tm, channel, configs);
					result = true;
				} catch(Throwable t) {
					if(logger.isErrorEnabled())
						logger.error("Exception in readChannelTem for <" + channel + ">", t);
				} finally{
					if(logger.isInfoEnabled())
						logger.info("End read readChannelTem for <" + channel.getId() + "> used <" + (System.currentTimeMillis() - tmp) + "> result <" + result + ">");
				}
			}
		
		// close device
		closeTemMonitor(machine.getId(), tm);
	}

	private void readChannelTem(TemMonitor tm, Channel channel, Map<Integer, Config> configs) throws IOException {
		double[] stock = new double[channel.getLength()];
		double[] unstock = new double[channel.getLength()];
		double[] referTem = new double[1];
		double[] tem = new double[channel.getLength()];
		
		pickData(tm, channel.getId(), NumberUtils.toInt(channel.getName(), -1), channel.getLength(), stock, unstock, referTem, tem);
		
		StringBuilder sb = new StringBuilder();
		sb.append(channel.getId());
		sb.append(PinaoConstants.TEM_DATA_COL_SEP);
		for(double d : tem)
			sb.append(d + PinaoConstants.TEM_DATA_ELEMENT_SEP);
		sb.append(PinaoConstants.TEM_DATA_COL_SEP);
		Config config = configs.get(Config.TYPE_STOCK_FLAG);
		if(config != null && config.getValue() == Config.VALUE_SAVE){
			for(double d : stock)
				sb.append(d + PinaoConstants.TEM_DATA_ELEMENT_SEP);
			sb.append(PinaoConstants.TEM_DATA_COL_SEP);
			for(double d : unstock)
				sb.append(d + PinaoConstants.TEM_DATA_ELEMENT_SEP);
		}
		sb.append(PinaoConstants.TEM_DATA_COL_SEP);
		config = configs.get(Config.TYPE_REFER_TEM_FLAG);
		if(config != null && config.getValue() == Config.VALUE_SAVE)
			sb.append(referTem[0]);
		
		saveToTmpFile(sb);
	}

	private void saveToTmpFile(StringBuilder sb) throws IOException {
		long start = System.currentTimeMillis();
		boolean result = false;
		try{
			// save into file-read
	    	String prefix = "" + System.currentTimeMillis();
	    	File file = new File(path + "/" + prefix + PinaoConstants.TEM_READ_SUFFIX);
			FileWriter fw = new FileWriter(file);
			fw.write(sb.toString());
			fw.flush();
			fw.close();
			
	    	// rename file-read to file
			file.renameTo(new File(dir, prefix));
			result = true;
		} finally{
			if(logger.isInfoEnabled())
				logger.info("[DTS-DLL]SaveTmpFile used <" + (System.currentTimeMillis() - start) + "> result <" + result + ">");
		}
	}

	private void pickData(TemMonitor tm, int id, int name, int length, double[] stock, double[] unstock, double[] referTem, double[] tem) {
		int state = Integer.MAX_VALUE;
		long start = System.currentTimeMillis();
		boolean result = false;
		try{
			state = tm.PickData(name, length, stock, unstock, referTem, tem, 0);
			result = true;
		} finally{
			if(logger.isInfoEnabled())
				logger.info("[DTS-DLL]PickData returned <" + state + "> for channel.id <" + id + "> used <" + (System.currentTimeMillis() - start) + "> result <" + result + ">");
		}
	}

	private void closeTemMonitor(int id, TemMonitor tm) {
		long start = System.currentTimeMillis();
		boolean result = false;
		try{
			tm.CloseDevice();
			result = true;
		} finally{
			if(logger.isInfoEnabled())
				logger.info("[DTS-DLL]CloseDevice for machine.id <" + id + "> used <" + (System.currentTimeMillis() - start) + "> result <" + result + ">");
		}
	}
	
	private TemMonitor newTemMonitor(int seriaPort, int baudRate){
		TemMonitor tm = new TemMonitor();
		int state = Integer.MAX_VALUE;
		long start = System.currentTimeMillis();
		boolean result = false;
		try{
			state = tm.InitDts(seriaPort, baudRate);
			result = true;
		} finally{
			if(logger.isInfoEnabled())
				logger.info("[DTS-DLL]Init returned <" + state + "> for seriaPort <" + seriaPort + "> baudRate <" + baudRate + "> used <" + (System.currentTimeMillis() - start) + "> result <" + result + ">");
		}
		return tm;
	}

	private void loadDLL() {
		long start = System.currentTimeMillis();
		boolean result = false;
		try{
			System.loadLibrary("TemMonitor");
			result = true;
		} finally{
			if(logger.isInfoEnabled())
				logger.info("[DTS-DLL]load TemMonitor dll used <" + (System.currentTimeMillis() - start) + "> result <" + result + ">");
		}
	}

	public String getMachineId() {
		return machineId;
	}

	public void setMachineId(String machineId) {
		this.machineId = machineId;
	}

	public AreaDao getAreaDao() {
		return areaDao;
	}

	public void setAreaDao(AreaDao areaDao) {
		this.areaDao = areaDao;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public File getDir() {
		return dir;
	}

	public void setDir(File dir) {
		this.dir = dir;
	}

	public ConfigDao getConfigDao() {
		return configDao;
	}

	public void setConfigDao(ConfigDao configDao) {
		this.configDao = configDao;
	}

	public TemDao getTemDao() {
		return temDao;
	}

	public void setTemDao(TemDao temDao) {
		this.temDao = temDao;
	}
}
