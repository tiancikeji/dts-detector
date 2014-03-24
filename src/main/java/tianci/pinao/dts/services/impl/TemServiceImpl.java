package tianci.pinao.dts.services.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.commons.lang.time.DateUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.transaction.annotation.Transactional;

import tianci.pinao.dts.models.Alarm;
import tianci.pinao.dts.models.AlarmProtocol;
import tianci.pinao.dts.models.AreaChannel;
import tianci.pinao.dts.models.AreaHardwareConfig;
import tianci.pinao.dts.models.AreaTempConfig;
import tianci.pinao.dts.models.Channel;
import tianci.pinao.dts.models.Check;
import tianci.pinao.dts.models.Config;
import tianci.pinao.dts.models.Machine;
import tianci.pinao.dts.models.Temperature;
import tianci.pinao.dts.sal.TemAlarm;
import tianci.pinao.dts.sal.TemMonitor;
import tianci.pinao.dts.services.TemService;
import tianci.pinao.dts.tasks.dao.AlarmDao;
import tianci.pinao.dts.tasks.dao.AreaDao;
import tianci.pinao.dts.tasks.dao.ConfigDao;
import tianci.pinao.dts.tasks.dao.TemDao;
import tianci.pinao.dts.util.PinaoConstants;

public class TemServiceImpl implements TemService {
	
	private Log logger = LogFactory.getLog(getClass());

	private String machineId;
	
	private int eventTime = -7200000;
	
	private int eventStartTime = -3600000;
	
	private int eventEndTime = 3600000;
	
	private int logTime = 3600000;

	private String path = PinaoConstants.TEM_PATH;
	
	private File dir = new File(path);
	
	//*****//
	private AreaDao areaDao;
	
	private ConfigDao configDao;
	
	private TemDao temDao;
	
	private AlarmDao alarmDao;
	
	public TemServiceImpl(){
		// load DLL
		loadDLL();
		
    	if(!(dir.exists() && dir.isDirectory()))
    		dir.mkdirs();
	}

	@Override
	public void checkHardware() {
		if(logger.isInfoEnabled())
			logger.info("Starting check hardware");
		long start = System.currentTimeMillis();
		
		try{
			List<Machine> machines = areaDao.getMachineByName(machineId);
			if(machines != null && machines.size() > 0)
				for(Machine machine : machines){
					List<Check> checks = alarmDao.getChecks(machine.getId(), Check.STATUS_NEW);
					
					if(checks != null && checks.size() > 0)
						_checkHardware(checks, machine);
				}
		} finally{
			if(logger.isInfoEnabled())
				logger.info("End check Hardware used <" + (System.currentTimeMillis() - start) + ">");
		}
	}

	private void _checkHardware(List<Check> checks, Machine machine) {
		try{
			List<Integer> states = new ArrayList<Integer>();
			
			for(Check check : checks){
				states.add(NumberUtils.toInt(check.getLight()));
				states.add(AlarmProtocol.LIGHT_STATE_GLITCH_FIRE);
				states.add(NumberUtils.toInt(check.getRelay()));
				states.add(AlarmProtocol.RELAY_STATE_ON);
				states.add(NumberUtils.toInt(check.getRelay1()));
				states.add(AlarmProtocol.RELAY_STATE_ON);
				states.add(NumberUtils.toInt(check.getVoice()));
				states.add(AlarmProtocol.VOICE_STATE_GLITCH_FIRE);
			}
			
			states.add(0);
			
			// call DLL
			callAlarm(states, machine);
			
			states = new ArrayList<Integer>();
			
			for(Check check : checks){
				states.add(NumberUtils.toInt(check.getLight()));
				states.add(AlarmProtocol.LIGHT_STATE_OFF);
				states.add(NumberUtils.toInt(check.getRelay()));
				states.add(AlarmProtocol.RELAY_STATE_OFF);
				states.add(NumberUtils.toInt(check.getRelay1()));
				states.add(AlarmProtocol.RELAY_STATE_OFF);
				states.add(NumberUtils.toInt(check.getVoice()));
				states.add(AlarmProtocol.VOICE_STATE_OFF);
			}
			
			states.add(0);
			
			// call DLL
			callAlarm(states, machine);
		} finally{
			// restore states
			List<Long> ids = new ArrayList<Long>();
			ids.add(new Long(machine.getId()));
			
			List<Alarm> alarms = alarmDao.getAlarms(ids, new Object[]{Alarm.STATUS_ALARMED, Alarm.STATUS_NOTIFY, Alarm.STATUS_MUTE, Alarm.STATUS_MUTED, Alarm.STATUS_RESET});
			
			if(alarms != null && alarms.size() > 0){
				Collections.sort(alarms, new Comparator<Alarm>(){
					@Override
					public int compare(Alarm a1, Alarm a2) {
						return a1.getStatus() - a2.getStatus();
					}
				});
				_ctrlTem(alarms, machine, ids);
			}
			
			// update status
			alarmDao.updateChecks(checks, Check.STATUS_CHECK);
		}
	}

	@Override
	@Transactional(rollbackFor=Exception.class, value="txManager")
	public void eventTem() {
		if(logger.isInfoEnabled())
			logger.info("Starting event tem");
		long start = System.currentTimeMillis();
		try{
			// get data from temperature - last 2 hour's alarm time
			Date startDate = DateUtils.addMilliseconds(new Date(), eventTime);
			List<Date> alarmDates = alarmDao.getAlarmDates(startDate);
			
			if(alarmDates != null && alarmDates.size() > 0){
				Collections.sort(alarmDates, new Comparator<Date>() {
					@Override
					public int compare(Date d1, Date d2) {
						return d1.before(d2) ? 0 : 1;
					}
				});
				
				Date startTmp = DateUtils.addMilliseconds(alarmDates.get(0), eventStartTime);
				Date endTmp = DateUtils.addMilliseconds(alarmDates.get(alarmDates.size() - 1), eventEndTime);
				
				// event data
				temDao.copyTemToEvent(startTmp, endTmp);
				
				// remove tmp data
				temDao.removeTemFromTmp(startTmp, endTmp);
			}
			
			// remove tmp data
			temDao.removeTemFromTmp(startDate);
		} finally {
	    	if(logger.isInfoEnabled())
	    		logger.info("eventTem used <" + (System.currentTimeMillis() - start) + ">");	
		}
	}

	@Override
	@Transactional(rollbackFor=Exception.class, value="txManager")
	public void logTem() {
		if(logger.isInfoEnabled())
			logger.info("Starting log tem");
		long start = System.currentTimeMillis();
		try{
			// get data from temperature - 1 hour ago & status:1
			List<Machine> machines = areaDao.getMachineByName(machineId);
			if(machines != null && machines.size() > 0){
				List<Long> ids = new ArrayList<Long>();
				for(Machine machine : machines)
					ids.add(new Long(machine.getId()));
				
				List<Channel> channels = areaDao.getAllChannelsByMachineIds(ids);
				if(channels != null && channels.size() > 0){
					List<Integer> cIds = new ArrayList<Integer>();
					for(Channel channel : channels)
						cIds.add(channel.getId());
					
					Date endDate = DateUtils.addMilliseconds(new Date(), logTime);
					List<Temperature> tems = temDao.getTemsByDateNStatus(cIds, endDate, Temperature.STATUS_ALARMED);
					if(tems != null && tems.size() > 0){
						// get log interval
						Config config = configDao.getConfigs().get(Config.TYPE_BACK_INTERVAL_FLAG);
						
						// NOTE: no back up by default!!!
						if(config != null && config.getValue() > 0){
							int logInterval = new Long(config.getValue()).intValue();
							
							// filter log data
							Map<Integer, List<Temperature>> tmps = groupTempsByChannel(tems);
							
							List<Temperature> logs = new ArrayList<Temperature>();
							Map<Integer, Date> maxDates = temDao.getMaxDateFromLog(cIds);
							
							for(Integer channel : tmps.keySet()){
								Date maxDate = maxDates.get(channel);
								if(maxDate != null)
									maxDate = DateUtils.addMinutes(maxDate, logInterval);
								
								List<Temperature> tmp = tmps.get(channel);
								
								if(tmp != null && tmps.size() > 0)
									for(Temperature tem : tmp)
										if(tem != null && tem.getDate() != null && (maxDate == null || maxDate.before(tem.getDate()))){
											logs.add(tem);
											maxDate = DateUtils.addMinutes(tem.getDate(), logInterval);
										} else{
											if(alarmDao.hasAlarm(tem.getChannel(), DateUtils.addHours(tem.getDate(), -1), DateUtils.addHours(tem.getDate(), 1))){
												logs.add(tem);
												maxDate = DateUtils.addMinutes(tem.getDate(), logInterval);
											}
										}
							}
							
							// insert filtered into log
							if(logs != null && logs.size() > 0){
								Config configStock = configDao.getConfigs().get(Config.TYPE_STOCK_FLAG);
								boolean stock = false;
								if(configStock != null && configStock.getValue() == Config.VALUE_SAVE)
									stock = true;

								Config configRefer= configDao.getConfigs().get(Config.TYPE_REFER_TEM_FLAG);
								boolean refer = false;
								if(configRefer != null && configRefer.getValue() == Config.VALUE_SAVE)
									refer = true;
								
								temDao.copyTemToLog(logs, stock, refer);
							}
						}
						
						// insert all into tmp
						//temDao.copyTemToTmp(endDate, Temperature.STATUS_ALARMED);
						
						// remove from temperature
						temDao.removeTemFromTem(endDate, Temperature.STATUS_ALARMED);
					}
				}
			}
		} finally {
	    	if(logger.isInfoEnabled())
	    		logger.info("logTem used <" + (System.currentTimeMillis() - start) + ">");	
		}
	}

	private Map<Integer, List<Temperature>> groupTempsByChannel(List<Temperature> tems) {
		Map<Integer, List<Temperature>> tmps = new HashMap<Integer, List<Temperature>>();
		for(Temperature tem : tems)
			if(tem != null){
				List<Temperature> _tmp = tmps.get(tem.getChannel());
				if(_tmp == null){
					_tmp = new ArrayList<Temperature>();
					tmps.put(tem.getChannel(), _tmp);
				}
				_tmp.add(tem);
			}
		
		// sort
		for(Integer key : tmps.keySet())
			Collections.sort(tmps.get(key), new Comparator<Temperature>() {
				@Override
				public int compare(Temperature t1, Temperature t2) {
					Date d1 = t1.getDate();
					Date d2 = t2.getDate();
					return d1.before(d2) ? 0 : 1;
				}
			});
		return tmps;
	}

	@Override
	public void saveTem() {
		if(logger.isInfoEnabled())
			logger.info("Starting save tem");
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
		
		List<Temperature> tems = null;
		try{
			// read file-data
			tems = readTemDataFromFile(path);
			
			Date date = null;
			long tmp = NumberUtils.toLong(StringUtils.removeEnd(path.getName(), PinaoConstants.TEM_WRITE_SUFFIX), -1);
			if(tmp <= 0)
				date = new Date();
			else
				date = new Date(tmp);
			
			// save into db
			if(tems != null && tems.size() > 0){
				for(Temperature _temp : tems)
					_temp.setDate(date);
				temDao.saveTems(tems, date);
			}
		} catch(Throwable t){
			// rename back...
			path.renameTo(file);
			throw t;
		}
		
		// delete files
		path.delete();
		
		// check & alarm logic
		/*if(tems != null && tems.size() > 0){
			long start = System.currentTimeMillis();
			try{
				checkNAlarm(tems);
			} catch(Throwable t){
				logger.error("Error in checkNAlarm >>", t);
			} finally{
				if(logger.isInfoEnabled())
					logger.info("End checkNAlarm used <" + (System.currentTimeMillis() - start) + ">");
			}
		}*/
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
					tem.setStatus(Temperature.STATUS_NEW);
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
					//Map<Integer, Config> configs = configDao.getConfigs();
					for(Machine machine : machines){
						long tmp = System.currentTimeMillis();
						boolean result = false;
						try{
							readMachineTem(machine/*, configs*/);
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

	private void readMachineTem(Machine machine/*, Map<Integer, Config> configs*/) {
		// open device
		TemMonitor tm = newTemMonitor(NumberUtils.toInt(machine.getSerialPort(), -1), NumberUtils.toInt(machine.getBaudRate(), -1));
		
		// get channels
		List<Channel> channels = areaDao.getAllChannelsByMachineId(machine.getId());
		
		if(channels != null && channels.size() > 0)
			for(Channel channel : channels){
				long tmp = System.currentTimeMillis();
				boolean result = false;
				try{
			    	readChannelTem(tm, channel/*, configs*/);
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

	private void readChannelTem(TemMonitor tm, Channel channel/*, Map<Integer, Config> configs*/) throws IOException {
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
		//Config config = configs.get(Config.TYPE_STOCK_FLAG);
		//if(config != null && config.getValue() == Config.VALUE_SAVE){
		for(double d : stock)
			sb.append(d + PinaoConstants.TEM_DATA_ELEMENT_SEP);
		sb.append(PinaoConstants.TEM_DATA_COL_SEP);
		for(double d : unstock)
			sb.append(d + PinaoConstants.TEM_DATA_ELEMENT_SEP);
		//}
		sb.append(PinaoConstants.TEM_DATA_COL_SEP);
		//config = configs.get(Config.TYPE_REFER_TEM_FLAG);
		//if(config != null && config.getValue() == Config.VALUE_SAVE)
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
			/*tm.CloseDevice();*/
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
			System.loadLibrary("Alarm");
			result = true;
		} finally{
			if(logger.isInfoEnabled())
				logger.info("[DTS-DLL]load TemMonitor dll used <" + (System.currentTimeMillis() - start) + "> result <" + result + ">");
		}
	}

	@Override
	public void checkTem() {
		if(logger.isInfoEnabled())
			logger.info("Starting check tem");
		long start = System.currentTimeMillis();
		
		try{
			List<Machine> machines = areaDao.getMachineByName(machineId);
			if(machines != null && machines.size() > 0){
				List<Long> ids = new ArrayList<Long>();
				for(Machine machine : machines)
					ids.add(new Long(machine.getId()));
				
				List<Channel> channels = areaDao.getAllChannelsByMachineIds(ids);
				if(channels != null && channels.size() > 0){
					List<Integer> cIds = new ArrayList<Integer>();
					for(Channel channel : channels)
						cIds.add(channel.getId());
					
					List<Temperature> tems = temDao.getTemsByStatus(cIds, Temperature.STATUS_NEW);
					
					if(tems != null && tems.size() > 0)
						_checkTem(tems);
				}
			}
		} finally{
			if(logger.isInfoEnabled())
				logger.info("End check tem used <" + (System.currentTimeMillis() - start) + ">");
		}
	}

	public void _checkTem(List<Temperature> tems) {
		// check alarm logic
		if(tems != null && tems.size() > 0){
			// channel ids
			Set<Integer> cIds = new HashSet<Integer>();
			for(Temperature tem : tems)
				cIds.add(tem.getChannel());
			
			// area channels
			Map<Integer, List<AreaChannel>> aChannels = areaDao.getAChannelsByIds(cIds);
			Set<Integer> aIds = new HashSet<Integer>();
			if(aChannels != null && aChannels.size() > 0)
				for(Integer cId : aChannels.keySet())
					for(AreaChannel ac : aChannels.get(cId))
						aIds.add(ac.getAreaid());
			
			// area temp configs
			Map<Integer, AreaTempConfig> aTemps = areaDao.getATempsByIds(aIds);
			
			// area hardware configs
			Map<Integer, AreaHardwareConfig> aHards = areaDao.getAHardsByIds(aIds);
			
			// configs
			Config configLow = configDao.getConfigs().get(Config.TYPE_TEMPERATURE_EXTREME_LOW);
			Config configHigh = configDao.getConfigs().get(Config.TYPE_TEMPERATURE_EXTREME_HIGH);
			Config configStock = configDao.getConfigs().get(Config.TYPE_STOCK_THRELHOLD);
			Config configUnstock = configDao.getConfigs().get(Config.TYPE_UNSTOCK_THRELHOLD);
			
			List<Alarm> alarms = new ArrayList<Alarm>();
			double max = 0;
			// check as can as possible
			for(Temperature tem : tems)
				try{
					String[] _tems = StringUtils.split(tem.getTem(), PinaoConstants.TEM_DATA_ELEMENT_SEP);
					String[] _stocks = StringUtils.split(tem.getStock(), PinaoConstants.TEM_DATA_ELEMENT_SEP);
					String[] _unstocks = StringUtils.split(tem.getUnstock(), PinaoConstants.TEM_DATA_ELEMENT_SEP);
					List<AreaChannel> _channels = aChannels.get(tem.getChannel());
	
					// previous tems
					List<Temperature> _preTems = temDao.getTemsByIds(tem.getChannel(), tem.getDate(), 5);
					String[] _preTem1 = null;
					String[] _preTem2 = null;
					String[] _preTem3 = null;
					String[] _preTem4 = null;
					String[] _preTem5 = null;
					String[] _preStocks1 = null;
					String[] _preStocks2 = null;
					String[] _preStocks3 = null;
					String[] _preStocks4 = null;
					String[] _preStocks5 = null;
					String[] _preUnstocks1 = null;
					String[] _preUnstocks2 = null;
					String[] _preUnstocks3 = null;
					String[] _preUnstocks4 = null;
					String[] _preUnstocks5 = null;
					Date date1 = null;
					Date date2 = null;
					Date date3 = null;
					Date date4 = null;
					Date date5 = null;
					if(_preTems != null && _preTems.size() > 0){
						_preTem1 = StringUtils.split(_preTems.get(0).getTem(), PinaoConstants.TEM_DATA_ELEMENT_SEP);
						_preStocks1 = StringUtils.split(_preTems.get(0).getStock(), PinaoConstants.TEM_DATA_ELEMENT_SEP);
						_preUnstocks1 = StringUtils.split(_preTems.get(0).getUnstock(), PinaoConstants.TEM_DATA_ELEMENT_SEP);
						date1 = _preTems.get(0).getDate();
	
						if(_preTems.size() > 4){
							_preTem2 = StringUtils.split(_preTems.get(1).getTem(), PinaoConstants.TEM_DATA_ELEMENT_SEP);
							_preTem3 = StringUtils.split(_preTems.get(2).getTem(), PinaoConstants.TEM_DATA_ELEMENT_SEP);
							_preTem4 = StringUtils.split(_preTems.get(3).getTem(), PinaoConstants.TEM_DATA_ELEMENT_SEP);
							_preTem5 = StringUtils.split(_preTems.get(4).getTem(), PinaoConstants.TEM_DATA_ELEMENT_SEP);
							_preStocks2 = StringUtils.split(_preTems.get(1).getStock(), PinaoConstants.TEM_DATA_ELEMENT_SEP);
							_preStocks3 = StringUtils.split(_preTems.get(2).getStock(), PinaoConstants.TEM_DATA_ELEMENT_SEP);
							_preStocks4 = StringUtils.split(_preTems.get(3).getStock(), PinaoConstants.TEM_DATA_ELEMENT_SEP);
							_preStocks5 = StringUtils.split(_preTems.get(4).getStock(), PinaoConstants.TEM_DATA_ELEMENT_SEP);
							_preUnstocks2 = StringUtils.split(_preTems.get(1).getUnstock(), PinaoConstants.TEM_DATA_ELEMENT_SEP);
							_preUnstocks3 = StringUtils.split(_preTems.get(2).getUnstock(), PinaoConstants.TEM_DATA_ELEMENT_SEP);
							_preUnstocks4 = StringUtils.split(_preTems.get(3).getUnstock(), PinaoConstants.TEM_DATA_ELEMENT_SEP);
							_preUnstocks5 = StringUtils.split(_preTems.get(4).getUnstock(), PinaoConstants.TEM_DATA_ELEMENT_SEP);
							date2 = _preTems.get(1).getDate();
							date3 = _preTems.get(2).getDate();
							date4 = _preTems.get(3).getDate();
							date5 = _preTems.get(4).getDate();
						}
					}
				
					if(_tems != null && _tems.length > 0 && _channels != null && _channels.size() > 0){
						Map<Integer, Map<Integer, Double>> _currAreaTems = new HashMap<Integer, Map<Integer,Double>>();
						Map<Integer, Map<Integer, Double>> _preAreaTems = new HashMap<Integer, Map<Integer,Double>>();
						
						for(int i = 0; i <= _tems.length; i ++){
							AreaChannel _channel = findAreaChannel(_channels, i + 1);
							if(_channel != null){
								AreaTempConfig tempConfig = aTemps.get(_channel.getAreaid());
								AreaHardwareConfig hardWareConfig = aHards.get(_channel.getAreaid());
								Double _tem = NumberUtils.toDouble(_tems[i], -1);
								
								if(_tem > max)
									max = _tem;
								
								// firstly check error
								boolean glitch = false;
								
								// check tem > -40 & < 120
								try{
									if(checkLowGlitch(configLow, _tem)){
										Alarm alarm = new Alarm();
										
										alarm.setAlarmName(_channel.getName());
										alarm.setAreaId(_channel.getAreaid());
										alarm.setAreaName(_channel.getAreaName());
										alarm.setChannelId(_channel.getChannelid());
										alarm.setChannelName(_channel.getChannelName());
										alarm.setLength(i + 1);
										if(hardWareConfig != null){
											alarm.setLight(hardWareConfig.getLight());
											alarm.setRelay(hardWareConfig.getRelay());
											alarm.setRelay1(hardWareConfig.getRelay1());
											alarm.setVoice(hardWareConfig.getVoice());
										}
										alarm.setMachineId(_channel.getMachineid());
										alarm.setMachineName(_channel.getMachineName());
										alarm.setStatus(Alarm.STATUS_NEW);
										alarm.setTemperatureCurr(_tem);
										alarm.setTemperaturePre(configLow.getValue());
										alarm.setType(Alarm.TYPE_TEMPERATURE_EXTREME_LOW);
		
										// alarmDao.addAlarm(alarm);
										alarms.add(alarm);
										glitch = true;
									} else if(checkHighGlitch(configHigh, _tem)){
										Alarm alarm = new Alarm();
										
										alarm.setAlarmName(_channel.getName());
										alarm.setAreaId(_channel.getAreaid());
										alarm.setAreaName(_channel.getAreaName());
										alarm.setChannelId(_channel.getChannelid());
										alarm.setChannelName(_channel.getChannelName());
										alarm.setLength(i + 1);
										if(hardWareConfig != null){
											alarm.setLight(hardWareConfig.getLight());
											alarm.setRelay(hardWareConfig.getRelay());
											alarm.setRelay1(hardWareConfig.getRelay1());
											alarm.setVoice(hardWareConfig.getVoice());
										}
										alarm.setMachineId(_channel.getMachineid());
										alarm.setMachineName(_channel.getMachineName());
										alarm.setStatus(Alarm.STATUS_NEW);
										alarm.setTemperatureCurr(_tem);
										alarm.setTemperaturePre(configHigh.getValue());
										alarm.setType(Alarm.TYPE_TEMPERATURE_EXTREME_HIGH);
		
										// alarmDao.addAlarm(alarm);
										alarms.add(alarm);
										glitch = true;
									}
								} catch(Throwable t){
									logger.error("Error in checking tem glitch >> ", t);
								}
								
								// check stock & unstock < fixed value
								try{
									Double _stock = NumberUtils.toDouble(_stocks[i], -1);
									if(checkLowGlitch(configStock, _stock)){
										Alarm alarm = new Alarm();
										
										alarm.setAlarmName(_channel.getName());
										alarm.setAreaId(_channel.getAreaid());
										alarm.setAreaName(_channel.getAreaName());
										alarm.setChannelId(_channel.getChannelid());
										alarm.setChannelName(_channel.getChannelName());
										alarm.setLength(i + 1);
										if(hardWareConfig != null){
											alarm.setLight(hardWareConfig.getLight());
											alarm.setRelay(hardWareConfig.getRelay());
											alarm.setRelay1(hardWareConfig.getRelay1());
											alarm.setVoice(hardWareConfig.getVoice());
										}
										alarm.setMachineId(_channel.getMachineid());
										alarm.setMachineName(_channel.getMachineName());
										alarm.setStatus(Alarm.STATUS_NEW);
										alarm.setTemperatureCurr(_stock);
										alarm.setTemperaturePre(configStock.getValue());
										alarm.setType(Alarm.TYPE_STOCK_LOW);
		
										// alarmDao.addAlarm(alarm);
										alarms.add(alarm);
										glitch = true;
									}
								} catch(Throwable t){
									logger.error("Error in checking stock glitch >> ", t);
								}
								
								try{
									Double _unstock = NumberUtils.toDouble(_unstocks[i], -1);
									if(checkLowGlitch(configUnstock, _unstock)){
										Alarm alarm = new Alarm();
										
										alarm.setAlarmName(_channel.getName());
										alarm.setAreaId(_channel.getAreaid());
										alarm.setAreaName(_channel.getAreaName());
										alarm.setChannelId(_channel.getChannelid());
										alarm.setChannelName(_channel.getChannelName());
										alarm.setLength(i + 1);
										if(hardWareConfig != null){
											alarm.setLight(hardWareConfig.getLight());
											alarm.setRelay(hardWareConfig.getRelay());
											alarm.setRelay1(hardWareConfig.getRelay1());
											alarm.setVoice(hardWareConfig.getVoice());
										}
										alarm.setMachineId(_channel.getMachineid());
										alarm.setMachineName(_channel.getMachineName());
										alarm.setStatus(Alarm.STATUS_NEW);
										alarm.setTemperatureCurr(_unstock);
										alarm.setTemperaturePre(configUnstock.getValue());
										alarm.setType(Alarm.TYPE_UNSTOCK_LOW);
		
										// alarmDao.addAlarm(alarm);
										alarms.add(alarm);
										glitch = true;
									}
								} catch(Throwable t){
									logger.error("Error in checking unstock glitch >> ", t);
								}
								
								// glitch, then continue;
								if(glitch)
									continue;
								
								if(tempConfig != null && _preTem1 != null && _preTem1.length > i){
									// check last glitch
									double _preTemp1 = NumberUtils.toDouble(_preTem1[i], -1);
									try{
										if(checkLowGlitch(configLow, _preTemp1))
											continue;
										else if(checkHighGlitch(configHigh, _preTemp1))
											continue;
									} catch(Throwable t){
										logger.error("Error in checking last tem glitch >> ", t);
									}
	
									try{
										if(checkLowGlitch(configStock, NumberUtils.toDouble(_preStocks1[i], -1)))
											continue;
									} catch(Throwable t){
										logger.error("Error in checking last stock glitch >> ", t);
									}
									
									try{
										if(checkLowGlitch(configUnstock, NumberUtils.toDouble(_preUnstocks1[i], -1)))
											continue;
									} catch(Throwable t){
										logger.error("Error in checking last unstock glitch >> ", t);
									}
									
									// group tems
									try{
										Map<Integer, Double> _currentTems = _currAreaTems.get(_channel.getAreaid());
										if(_currentTems == null){
											_currentTems = new HashMap<Integer, Double>();
											_currAreaTems.put(_channel.getAreaid(), _currentTems);
										}
										_currentTems.put(i, _tem);
		
										_currentTems = _preAreaTems.get(_channel.getAreaid());
										if(_currentTems == null){
											_currentTems = new HashMap<Integer, Double>();
											_preAreaTems.put(_channel.getAreaid(), _currentTems);
										}
										_currentTems.put(i, _preTemp1);
									} catch(Throwable t){
										logger.error("Error in grouping tems >> ", t);
									}
									
									// 1. check tem_high -> tem_high_alarm
									try{
										if(_tem >= tempConfig.getTemperatureHigh() && _preTemp1 >= tempConfig.getTemperatureHigh()){
											Alarm alarm = new Alarm();
											
											alarm.setAlarmName(_channel.getName());
											alarm.setAreaId(_channel.getAreaid());
											alarm.setAreaName(_channel.getAreaName());
											alarm.setChannelId(_channel.getChannelid());
											alarm.setChannelName(_channel.getChannelName());
											alarm.setLength(i + 1);
											if(hardWareConfig != null){
												alarm.setLight(hardWareConfig.getLight());
												alarm.setRelay(hardWareConfig.getRelay());
												alarm.setRelay1(hardWareConfig.getRelay1());
												alarm.setVoice(hardWareConfig.getVoice());
											}
											alarm.setMachineId(_channel.getMachineid());
											alarm.setMachineName(_channel.getMachineName());
											alarm.setStatus(Alarm.STATUS_NEW);
											alarm.setTemperatureCurr(_tem);
											alarm.setTemperaturePre(tempConfig.getTemperatureHigh());
											alarm.setType(Alarm.TYPE_TEMPERATURE_HIGH);
		
											// alarmDao.addAlarm(alarm);
											alarms.add(alarm);
										} else if(_tem >= tempConfig.getTemperatureLow() && _preTemp1 >= tempConfig.getTemperatureLow()){
											// 2. check tem_low -> tem_low_alarm
											Alarm alarm = new Alarm();
											
											alarm.setAlarmName(_channel.getName());
											alarm.setAreaId(_channel.getAreaid());
											alarm.setAreaName(_channel.getAreaName());
											alarm.setChannelId(_channel.getChannelid());
											alarm.setChannelName(_channel.getChannelName());
											alarm.setLength(i + 1);
											if(hardWareConfig != null){
												alarm.setLight(hardWareConfig.getLight());
												alarm.setRelay(hardWareConfig.getRelay());
												alarm.setRelay1(hardWareConfig.getRelay1());
												alarm.setVoice(hardWareConfig.getVoice());
											}
											alarm.setMachineId(_channel.getMachineid());
											alarm.setMachineName(_channel.getMachineName());
											alarm.setStatus(Alarm.STATUS_NEW);
											alarm.setTemperatureCurr(_tem);
											alarm.setTemperaturePre(tempConfig.getTemperatureLow());
											alarm.setType(Alarm.TYPE_TEMPERATURE_LOW);
		
											// alarmDao.addAlarm(alarm);
											alarms.add(alarm);
										}
									} catch(Throwable t){
										logger.error("Error in checking tem low & high >> ", t);
									}
									
									// 3. check last 5 & current tem_rate -> tem_rate_alarm
									if(_preTem2 != null && _preTem2.length > i 
											&& _preTem3 != null && _preTem3.length > i
											&& _preTem4 != null && _preTem4.length > i
											&& _preTem5 != null && _preTem5.length > i
											&& tem.getDate() != null && date1 != null && date2 != null && date3 != null && date4 != null && date5 != null)
										try{
											// check last 5 glitch
											double _preTemp2 = NumberUtils.toDouble(_preTem2[i], -1);
											try{
												if(checkLowGlitch(configLow, _preTemp2))
													continue;
												else if(checkHighGlitch(configHigh, _preTemp2))
													continue;
											} catch(Throwable t){
												logger.error("Error in checking last tem2 glitch >> ", t);
											}
		
											try{
												if(checkLowGlitch(configStock, NumberUtils.toDouble(_preStocks2[i], -1)))
													continue;
											} catch(Throwable t){
												logger.error("Error in checking last stock2 glitch >> ", t);
											}
											
											try{
												if(checkLowGlitch(configUnstock, NumberUtils.toDouble(_preUnstocks2[i], -1)))
													continue;
											} catch(Throwable t){
												logger.error("Error in checking last unstock2 glitch >> ", t);
											}
											
											double _preTemp3 = NumberUtils.toDouble(_preTem3[i], -1);
											try{
												if(checkLowGlitch(configLow, _preTemp3))
													continue;
												else if(checkHighGlitch(configHigh, _preTemp3))
													continue;
											} catch(Throwable t){
												logger.error("Error in checking last tem3 glitch >> ", t);
											}
		
											try{
												if(checkLowGlitch(configStock, NumberUtils.toDouble(_preStocks3[i], -1)))
													continue;
											} catch(Throwable t){
												logger.error("Error in checking last stock3 glitch >> ", t);
											}
											
											try{
												if(checkLowGlitch(configUnstock, NumberUtils.toDouble(_preUnstocks3[i], -1)))
													continue;
											} catch(Throwable t){
												logger.error("Error in checking last unstock3 glitch >> ", t);
											}
											
											double _preTemp4 = NumberUtils.toDouble(_preTem4[i], -1);
											try{
												if(checkLowGlitch(configLow, _preTemp4))
													continue;
												else if(checkHighGlitch(configHigh, _preTemp4))
													continue;
											} catch(Throwable t){
												logger.error("Error in checking last tem4 glitch >> ", t);
											}
		
											try{
												if(checkLowGlitch(configStock, NumberUtils.toDouble(_preStocks4[i], -1)))
													continue;
											} catch(Throwable t){
												logger.error("Error in checking last stock4 glitch >> ", t);
											}
											
											try{
												if(checkLowGlitch(configUnstock, NumberUtils.toDouble(_preUnstocks4[i], -1)))
													continue;
											} catch(Throwable t){
												logger.error("Error in checking last unstock4 glitch >> ", t);
											}
											
											double _preTemp5 = NumberUtils.toDouble(_preTem5[i], -1);
											try{
												if(checkLowGlitch(configLow, _preTemp5))
													continue;
												else if(checkHighGlitch(configHigh, _preTemp5))
													continue;
											} catch(Throwable t){
												logger.error("Error in checking last tem5 glitch >> ", t);
											}
		
											try{
												if(checkLowGlitch(configStock, NumberUtils.toDouble(_preStocks5[i], -1)))
													continue;
											} catch(Throwable t){
												logger.error("Error in checking last stock5 glitch >> ", t);
											}
											
											try{
												if(checkLowGlitch(configUnstock, NumberUtils.toDouble(_preUnstocks5[i], -1)))
													continue;
											} catch(Throwable t){
												logger.error("Error in checking last unstock5 glitch >> ", t);
											}
											
											double _gap1 = _tem - _preTemp3;
											double _gap2 = _preTemp1 - _preTemp4;
											double _gap3 = _preTemp2 - _preTemp5;
											
											long _time1 = tem.getDate().getTime() - date3.getTime();
											long _time2 = date1.getTime() - date4.getTime();
											long _time3 = date2.getTime() - date5.getTime();
											
											if(_time1 > 0 && _time2 > 0 && _time3 > 0){
												double _exotherm1 = new BigDecimal(_gap1).multiply(new BigDecimal(60000)).divide(new BigDecimal(_time1), RoundingMode.HALF_UP).doubleValue();
												double _exotherm2 = new BigDecimal(_gap2).multiply(new BigDecimal(60000)).divide(new BigDecimal(_time2), RoundingMode.HALF_UP).doubleValue();
												double _exotherm3 = new BigDecimal(_gap3).multiply(new BigDecimal(60000)).divide(new BigDecimal(_time3), RoundingMode.HALF_UP).doubleValue();
												
												if(_exotherm1 >= tempConfig.getExotherm() && _exotherm2 >= tempConfig.getExotherm() && _exotherm3 >= tempConfig.getExotherm()){
													Alarm alarm = new Alarm();
													
													alarm.setAlarmName(_channel.getName());
													alarm.setAreaId(_channel.getAreaid());
													alarm.setAreaName(_channel.getAreaName());
													alarm.setChannelId(_channel.getChannelid());
													alarm.setChannelName(_channel.getChannelName());
													alarm.setLength(i + 1);
													if(hardWareConfig != null){
														alarm.setLight(hardWareConfig.getLight());
														alarm.setRelay(hardWareConfig.getRelay());
														alarm.setRelay1(hardWareConfig.getRelay1());
														alarm.setVoice(hardWareConfig.getVoice());
													}
													alarm.setMachineId(_channel.getMachineid());
													alarm.setMachineName(_channel.getMachineName());
													alarm.setStatus(Alarm.STATUS_NEW);
													alarm.setTemperatureCurr(_exotherm1);
													alarm.setTemperaturePre(tempConfig.getExotherm());
													alarm.setType(Alarm.TYPE_TEMPERATURE_EXOTHERM);
		
													// alarmDao.addAlarm(alarm);
													alarms.add(alarm);
												}
											}
										} catch(Throwable t){
											logger.error("Error in checking last 5 tem >> ", t);
										}
								}
							}
						}
						
						// 4. check last & current tem_gap_avg -> tem_gap_avg_alarm
						try{
							for(Integer areaId : _currAreaTems.keySet())
								try{
									if(_preAreaTems.containsKey(areaId)){
										Map<Integer, Double> _currAreaTem = _currAreaTems.get(areaId);
										Map<Integer, Double> _preAreaTem = _preAreaTems.get(areaId);
										AreaTempConfig tempConfig = aTemps.get(areaId);
										AreaHardwareConfig hardWareConfig = aHards.get(areaId);
										
										if(_currAreaTem != null && _currAreaTem.size() > 0 
												&& _preAreaTem != null && _preAreaTem.size() > 0 
												&& tempConfig != null){
											double currAvg = 0;
											for(Double _tmp : _currAreaTem.values())
												if(_tmp != null)
													currAvg += _tmp;
											currAvg = new BigDecimal(currAvg).divide(new BigDecimal(_currAreaTem.size()), RoundingMode.HALF_UP).doubleValue();
											currAvg += tempConfig.getTemperatureDiff();
											
											double preAvg = 0;
											for(Double _tmp : _preAreaTem.values())
												if(_tmp != null)
													preAvg += _tmp;
											preAvg = new BigDecimal(preAvg).divide(new BigDecimal(_preAreaTem.size()), RoundingMode.HALF_UP).doubleValue();
											preAvg += tempConfig.getTemperatureDiff();
											
											for(Integer length : _currAreaTem.keySet())
												try{
													if(_preAreaTem.containsKey(length)){
														AreaChannel _channel = findAreaChannel(_channels, length + 1);
														if(_channel != null){
															if(_currAreaTem.get(length) >= currAvg && _preAreaTem.get(length) >= preAvg){
																Alarm alarm = new Alarm();
																
																alarm.setAlarmName(_channel.getName());
																alarm.setAreaId(_channel.getAreaid());
																alarm.setAreaName(_channel.getAreaName());
																alarm.setChannelId(_channel.getChannelid());
																alarm.setChannelName(_channel.getChannelName());
																alarm.setLength(length + 1);
																if(hardWareConfig != null){
																	alarm.setLight(hardWareConfig.getLight());
																	alarm.setRelay(hardWareConfig.getRelay());
																	alarm.setRelay1(hardWareConfig.getRelay1());
																	alarm.setVoice(hardWareConfig.getVoice());
																}
																alarm.setMachineId(_channel.getMachineid());
																alarm.setMachineName(_channel.getMachineName());
																alarm.setStatus(Alarm.STATUS_NEW);
																alarm.setTemperatureCurr(_currAreaTem.get(length) - currAvg);
																alarm.setTemperaturePre(tempConfig.getTemperatureDiff());
																alarm.setType(Alarm.TYPE_TEMPERATURE_DIFF);
					
																alarms.add(alarm);
															}
														}
													}
												} catch(Throwable t){
													logger.error("error in checking tem_gap_avg for areaid <" + areaId + "> length <" + length + "> >> ", t);
												}
										}
									}
								} catch(Throwable t){
									logger.error("error in checking tem_gap_avg for areaid <" + areaId + "> >> ", t);
								}
						} catch(Throwable t){
							logger.error("error in checking tem_gap_avg >> ", t);
						}
					}
			} catch(Throwable t){
				logger.error("error in checking tems channel <" + tem.getChannel() + "> date <" + tem.getDate() + "> >> ", t);
			}
			
			// 7. check space
			try{
				Config configFree = configDao.getConfigs().get(Config.TYPE_FREE_SPACE_RATE);
				if(configFree != null){
					File file = new File("/");
					long free = file.getFreeSpace();
					long total = file.getTotalSpace();
					double rate = new BigDecimal(free).multiply(new BigDecimal(100)).divide(new BigDecimal(total), RoundingMode.HALF_UP).doubleValue();
					if(rate <= configFree.getValue())
						for(Integer cId : aChannels.keySet())
							for(AreaChannel _channel : aChannels.get(cId)){
								Alarm alarm = new Alarm();
								alarm.setAlarmName("硬盘容量");
								alarm.setAreaId(_channel.getAreaid());
								alarm.setAreaName(_channel.getAreaName());
								alarm.setChannelId(_channel.getChannelid());
								alarm.setChannelName(_channel.getChannelName());
								alarm.setLength(0);
								alarm.setMachineId(_channel.getMachineid());
								alarm.setMachineName(_channel.getMachineName());
								alarm.setStatus(Alarm.STATUS_ALARMED);
								alarm.setTemperatureCurr(rate);
								alarm.setTemperaturePre(configFree.getValue());
								alarm.setType(Alarm.TYPE_FREE_SPACE);
								
								alarms.add(alarm);
							}
				}
			} catch(Throwable t){
				logger.error("Error in checking space >> ", t);
			}
			
			// save alarms
			if(alarms != null && alarms.size() > 0){
				for(Alarm alarm : alarms)
					alarm.setTemperatureMax(max);
				alarmDao.addAlarms(alarms);
			}
			
			// update status
			temDao.updateTemsStatus(tems, Temperature.STATUS_ALARMED);
		}
	}

	public void initAlarm(){
		if(logger.isInfoEnabled())
			logger.info("Starting init alarm");
		long start = System.currentTimeMillis();
		
		try{
			List<Machine> machines = areaDao.getMachineByName(machineId);
			if(machines != null && machines.size() > 0)
				for(Machine machine : machines)
					_initAlarm(machine);
		} finally{
			if(logger.isInfoEnabled())
				logger.info("End init alarm used <" + (System.currentTimeMillis() - start) + ">");
		}
	}

	private void _initAlarm(Machine machine) {
		List<Long> ids = new ArrayList<Long>();
		ids.add(new Long(machine.getId()));
		
		Set<Integer> cIds = new HashSet<Integer>();
		List<Channel> channels = areaDao.getAllChannelsByMachineIds(ids);
		if(channels != null && channels.size() > 0){
			for(Channel channel : channels)
				cIds.add(channel.getId());
		}
		
		// area channels
		Map<Integer, List<AreaChannel>> aChannels = areaDao.getAChannelsByIds(cIds);
		Set<Integer> aIds = new HashSet<Integer>();
		if(aChannels != null && aChannels.size() > 0)
			for(Integer cId : aChannels.keySet())
				for(AreaChannel ac : aChannels.get(cId))
					aIds.add(ac.getAreaid());
		
		// area hardware configs
		Map<Integer, AreaHardwareConfig> aHards = areaDao.getAHardsByIds(aIds);

		List<Integer> states = new ArrayList<Integer>();
		for(AreaHardwareConfig config : aHards.values()){
			states.add(NumberUtils.toInt(config.getLight()));
			states.add(AlarmProtocol.LIGHT_STATE_OK);
			states.add(NumberUtils.toInt(config.getRelay()));
			states.add(AlarmProtocol.RELAY_STATE_OFF);
			states.add(NumberUtils.toInt(config.getRelay1()));
			states.add(AlarmProtocol.RELAY_STATE_OFF);
			states.add(NumberUtils.toInt(config.getVoice()));
			states.add(AlarmProtocol.VOICE_STATE_OFF);
		}
		
		states.add(0);
		
		// call DLL
		callAlarm(states, machine);
	}

	private boolean checkHighGlitch(Config config, double tem) {
		return config != null && tem >= config.getValue();
	}

	private boolean checkLowGlitch(Config config, double tem) {
		return config != null && tem <= config.getValue();
	}

	private AreaChannel findAreaChannel(List<AreaChannel> _channels, int index) {
		if(_channels != null && _channels.size() > 0)
			for(AreaChannel tmp : _channels)
				if(tmp != null && tmp.getStart() <= index && tmp.getEnd() >= index)
					return tmp;
		
		return null;
	}
	
	@Override
	public void ctrlTem() {
		if(logger.isInfoEnabled())
			logger.info("Starting ctrl tem");
		long start = System.currentTimeMillis();
		
		try{
			List<Machine> machines = areaDao.getMachineByName(machineId);
			if(machines != null && machines.size() > 0)
				for(Machine machine : machines){
					List<Long> ids = new ArrayList<Long>();
					ids.add(new Long(machine.getId()));
					
					List<Alarm> alarms = alarmDao.getAlarms(ids, new Object[]{Alarm.STATUS_ALARMED, Alarm.STATUS_NOTIFY, Alarm.STATUS_MUTE, Alarm.STATUS_MUTED, Alarm.STATUS_RESET});
					
					if(alarms != null && alarms.size() > 0){
						boolean ctrl = false;
						
						for(Alarm alarm : alarms)
							if(alarm.getStatus() == Alarm.STATUS_MUTE || alarm.getStatus() == Alarm.STATUS_RESET){
								ctrl = true;
								break;
							}
						
						if(ctrl){
							Collections.sort(alarms, new Comparator<Alarm>(){
								@Override
								public int compare(Alarm a1, Alarm a2) {
									return a1.getStatus() - a2.getStatus();
								}
							});
							_ctrlTem(alarms, machine, ids);
						}
					}
				}
		} finally{
			if(logger.isInfoEnabled())
				logger.info("End ctrl tem used <" + (System.currentTimeMillis() - start) + ">");
		}
	}

	// send alarm DLL
	private void _ctrlTem(List<Alarm> alarms, Machine machine, List<Long> ids) {
		if(alarms != null && alarms.size() > 0)
			try{
				Set<Integer> cIds = new HashSet<Integer>();
				List<Channel> channels = areaDao.getAllChannelsByMachineIds(ids);
				if(channels != null && channels.size() > 0){
					for(Channel channel : channels)
						cIds.add(channel.getId());
				}
				
				// area channels
				Map<Integer, List<AreaChannel>> aChannels = areaDao.getAChannelsByIds(cIds);
				Set<Integer> aIds = new HashSet<Integer>();
				if(aChannels != null && aChannels.size() > 0)
					for(Integer cId : aChannels.keySet())
						for(AreaChannel ac : aChannels.get(cId))
							aIds.add(ac.getAreaid());
				
				// area hardware configs
				Map<Integer, AreaHardwareConfig> aHards = areaDao.getAHardsByIds(aIds);
				
				boolean reset = false;
				double max = 0;
				for(Alarm alarm : alarms){
					if(alarm.getTemperatureMax() > max)
						max = alarm.getTemperatureMax();
					if(alarm.getStatus() == Alarm.STATUS_RESET){
						reset = true;
					}
				}
				List<Integer> states = new ArrayList<Integer>();
				
				if(!reset){
					Map<Integer, AlarmProtocol> protocols = new HashMap<Integer, AlarmProtocol>();
					for(Alarm alarm : alarms){
						if(alarm.getType() == Alarm.TYPE_FREE_SPACE)
							continue;
						
						AlarmProtocol _pro = protocols.get(alarm.getAreaId());
						if(_pro == null){
							_pro = new AlarmProtocol();
							_pro.setRelay(alarm.getRelay());
							_pro.setRelay1(alarm.getRelay1());
							_pro.setLight(alarm.getLight());
							_pro.setVoice(alarm.getVoice());
							
							protocols.put(alarm.getAreaId(), _pro);
							aHards.remove(alarm.getAreaId());
						}
						
						if(alarm.getType() == Alarm.TYPE_TEMPERATURE_LOW){
							_pro.setRelay1State(AlarmProtocol.RELAY_STATE_ON);
							
							if(_pro.getLightState() == AlarmProtocol.LIGHT_STATE_GLITCH
									|| _pro.getLightState() == AlarmProtocol.LIGHT_STATE_GLITCH_FIRE)
								_pro.setLightState(AlarmProtocol.LIGHT_STATE_GLITCH_FIRE);
							else
								_pro.setLightState(AlarmProtocol.LIGHT_STATE_FIRE);
							
							if(_pro.getVoiceState() == AlarmProtocol.VOICE_STATE_GLITCH
									|| _pro.getVoiceState() == AlarmProtocol.VOICE_STATE_GLITCH_FIRE)
								_pro.setVoiceState(AlarmProtocol.VOICE_STATE_GLITCH_FIRE);
							else
								_pro.setVoiceState(AlarmProtocol.VOICE_STATE_FIRE);
						}
						
						if(alarm.getType() == Alarm.TYPE_TEMPERATURE_HIGH
								|| alarm.getType() == Alarm.TYPE_TEMPERATURE_EXOTHERM
								|| alarm.getType() == Alarm.TYPE_TEMPERATURE_DIFF){
							_pro.setRelayState(AlarmProtocol.RELAY_STATE_ON);
							
							if(_pro.getLightState() == AlarmProtocol.LIGHT_STATE_GLITCH
									|| _pro.getLightState() == AlarmProtocol.LIGHT_STATE_GLITCH_FIRE)
								_pro.setLightState(AlarmProtocol.LIGHT_STATE_GLITCH_FIRE);
							else
								_pro.setLightState(AlarmProtocol.LIGHT_STATE_FIRE);
							
							if(_pro.getVoiceState() == AlarmProtocol.VOICE_STATE_GLITCH
									|| _pro.getVoiceState() == AlarmProtocol.VOICE_STATE_GLITCH_FIRE)
								_pro.setVoiceState(AlarmProtocol.VOICE_STATE_GLITCH_FIRE);
							else
								_pro.setVoiceState(AlarmProtocol.VOICE_STATE_FIRE);
						}
						
						if(alarm.getType() == Alarm.TYPE_TEMPERATURE_EXTREME_HIGH
								|| alarm.getType() == Alarm.TYPE_TEMPERATURE_EXTREME_LOW
								|| alarm.getType() == Alarm.TYPE_STOCK_LOW
								|| alarm.getType() == Alarm.TYPE_UNSTOCK_LOW){
							_pro.setRelayState(AlarmProtocol.RELAY_STATE_ON);
							
							if(_pro.getLightState() == AlarmProtocol.LIGHT_STATE_GLITCH
									|| _pro.getLightState() == AlarmProtocol.LIGHT_STATE_GLITCH_FIRE)
								_pro.setLightState(AlarmProtocol.LIGHT_STATE_GLITCH_FIRE);
							else
								_pro.setLightState(AlarmProtocol.LIGHT_STATE_GLITCH);
							
							if(_pro.getVoiceState() == AlarmProtocol.VOICE_STATE_GLITCH
									|| _pro.getVoiceState() == AlarmProtocol.VOICE_STATE_GLITCH_FIRE)
								_pro.setVoiceState(AlarmProtocol.VOICE_STATE_GLITCH_FIRE);
							else
								_pro.setVoiceState(AlarmProtocol.VOICE_STATE_GLITCH);
						}
						
						if(alarm.getStatus() == Alarm.STATUS_MUTE || alarm.getStatus() == Alarm.STATUS_MUTED){
							_pro.setVoiceState(AlarmProtocol.VOICE_STATE_OFF);
							alarm.setStatus(Alarm.STATUS_MUTED);
						}
					}
					
					for(AlarmProtocol pro : protocols.values()){
						states.add(NumberUtils.toInt(pro.getLight()));
						states.add(pro.getLightState());
						states.add(NumberUtils.toInt(pro.getRelay()));
						states.add(pro.getRelayState());
						states.add(NumberUtils.toInt(pro.getRelay1()));
						states.add(pro.getRelay1State());
						states.add(NumberUtils.toInt(pro.getVoice()));
						states.add(pro.getVoiceState());
					}
				} else
					for(Alarm alarm : alarms)
						alarm.setStatus(Alarm.STATUS_RESETED);
				
				for(AreaHardwareConfig config : aHards.values()){
					states.add(NumberUtils.toInt(config.getLight()));
					states.add(AlarmProtocol.LIGHT_STATE_OK);
					states.add(NumberUtils.toInt(config.getRelay()));
					states.add(AlarmProtocol.RELAY_STATE_OFF);
					states.add(NumberUtils.toInt(config.getRelay1()));
					states.add(AlarmProtocol.RELAY_STATE_OFF);
					states.add(NumberUtils.toInt(config.getVoice()));
					states.add(AlarmProtocol.VOICE_STATE_OFF);
				}
				
				states.add(new Double(max).intValue());
				
				// call DLL
				callAlarm(states, machine);
				
				// update alarms
				alarmDao.updateAlarms(alarms);
			} catch(Throwable t){
				logger.error("Error in sending alarm DLL >> ", t);
			}
	}
	
	private void callAlarm(List<Integer> states, Machine machine) {
		// init dll
		TemAlarm alarm = null;
		long start = System.currentTimeMillis();
		boolean result = false;
		try{
			alarm = new TemAlarm();
			result = alarm.InitPort(NumberUtils.toInt(machine.getSerialPort()), NumberUtils.toInt(machine.getBaudRate()));
		} finally{
			if(logger.isInfoEnabled())
				logger.info("[DTS-ALARM-DLL]InitPort for machine.id <" + machine.getId() + "> used <" + (System.currentTimeMillis() - start) + "> result <" + result + ">");
		}
		
		// send data
		start = System.currentTimeMillis();
		result = false;
		try{
			if(alarm != null){
				int[] _states = new int[states.size()];
				for(int i = 0; i < states.size(); i ++)
					_states[i] = states.get(i);
				
				result = alarm.SendData(0, _states, states.size());
			}
		} finally{
			if(logger.isInfoEnabled())
				logger.info("[DTS-ALARM-DLL]SendData for machine.id <" + machine.getId() + "> used <" + (System.currentTimeMillis() - start) + "> result <" + result + ">");
		}
		
		// close dll
		start = System.currentTimeMillis();
		result = false;
		try{
			if(alarm != null){
				alarm.ClosePort();
				result = true;
			}
		} finally{
			if(logger.isInfoEnabled())
				logger.info("[DTS-ALARM-DLL]ClosePort for machine.id <" + machine.getId() + "> used <" + (System.currentTimeMillis() - start) + "> result <" + result + ">");
		}
	}

	@Override
	public void alarmTem() {
		if(logger.isInfoEnabled())
			logger.info("Starting alarm tem");
		long start = System.currentTimeMillis();
		
		try{
			List<Machine> machines = areaDao.getMachineByName(machineId);
			if(machines != null && machines.size() > 0)
				for(Machine machine : machines){
					List<Long> ids = new ArrayList<Long>();
					ids.add(new Long(machine.getId()));
					
					List<Alarm> alarms = alarmDao.getAlarms(ids, new Object[]{Alarm.STATUS_NEW});
					
					if(alarms != null && alarms.size() > 0)
						_alarmTem(alarms, machine, ids);
				}
		} finally{
			if(logger.isInfoEnabled())
				logger.info("End alarm tem used <" + (System.currentTimeMillis() - start) + ">");
		}
	}

	// send alarm DLL
	private void _alarmTem(List<Alarm> alarms, Machine machine, List<Long> ids) {
		if(alarms != null && alarms.size() > 0)
			try{
				// channel ids
				Set<Integer> cIds = new HashSet<Integer>();
				for(Alarm alarm : alarms)
					cIds.add(alarm.getChannelId());
				
				// area channels
				Map<Integer, List<AreaChannel>> aChannels = areaDao.getAChannelsByIds(cIds);
				Set<Integer> aIds = new HashSet<Integer>();
				if(aChannels != null && aChannels.size() > 0)
					for(Integer cId : aChannels.keySet())
						for(AreaChannel ac : aChannels.get(cId))
							aIds.add(ac.getAreaid());
				
				// area hardware configs
				Map<Integer, AreaHardwareConfig> aHards = areaDao.getAHardsByIds(aIds);
				
				double max = 0;
				Map<Integer, AlarmProtocol> protocols = new HashMap<Integer, AlarmProtocol>();
				for(Alarm alarm : alarms){
					if(alarm.getTemperatureMax() > max)
						max = alarm.getTemperatureMax();
					AlarmProtocol _pro = protocols.get(alarm.getAreaId());
					if(_pro == null){
						_pro = new AlarmProtocol();
						_pro.setRelay(alarm.getRelay());
						_pro.setRelay1(alarm.getRelay1());
						_pro.setLight(alarm.getLight());
						_pro.setVoice(alarm.getVoice());
						
						protocols.put(alarm.getAreaId(), _pro);
						aHards.remove(alarm.getAreaId());
					}
					
					if(alarm.getType() == Alarm.TYPE_TEMPERATURE_LOW){
						_pro.setRelay1State(AlarmProtocol.RELAY_STATE_ON);
						
						if(_pro.getLightState() == AlarmProtocol.LIGHT_STATE_GLITCH
								|| _pro.getLightState() == AlarmProtocol.LIGHT_STATE_GLITCH_FIRE)
							_pro.setLightState(AlarmProtocol.LIGHT_STATE_GLITCH_FIRE);
						else
							_pro.setLightState(AlarmProtocol.LIGHT_STATE_FIRE);
						
						if(_pro.getVoiceState() == AlarmProtocol.VOICE_STATE_GLITCH
								|| _pro.getVoiceState() == AlarmProtocol.VOICE_STATE_GLITCH_FIRE)
							_pro.setVoiceState(AlarmProtocol.VOICE_STATE_GLITCH_FIRE);
						else
							_pro.setVoiceState(AlarmProtocol.VOICE_STATE_FIRE);
					}
					
					if(alarm.getType() == Alarm.TYPE_TEMPERATURE_HIGH
							|| alarm.getType() == Alarm.TYPE_TEMPERATURE_EXOTHERM
							|| alarm.getType() == Alarm.TYPE_TEMPERATURE_DIFF){
						_pro.setRelayState(AlarmProtocol.RELAY_STATE_ON);
						
						if(_pro.getLightState() == AlarmProtocol.LIGHT_STATE_GLITCH
								|| _pro.getLightState() == AlarmProtocol.LIGHT_STATE_GLITCH_FIRE)
							_pro.setLightState(AlarmProtocol.LIGHT_STATE_GLITCH_FIRE);
						else
							_pro.setLightState(AlarmProtocol.LIGHT_STATE_FIRE);
						
						if(_pro.getVoiceState() == AlarmProtocol.VOICE_STATE_GLITCH
								|| _pro.getVoiceState() == AlarmProtocol.VOICE_STATE_GLITCH_FIRE)
							_pro.setVoiceState(AlarmProtocol.VOICE_STATE_GLITCH_FIRE);
						else
							_pro.setVoiceState(AlarmProtocol.VOICE_STATE_FIRE);
					}
					
					if(alarm.getType() == Alarm.TYPE_TEMPERATURE_EXTREME_HIGH
							|| alarm.getType() == Alarm.TYPE_TEMPERATURE_EXTREME_LOW
							|| alarm.getType() == Alarm.TYPE_STOCK_LOW
							|| alarm.getType() == Alarm.TYPE_UNSTOCK_LOW){
						_pro.setRelayState(AlarmProtocol.RELAY_STATE_ON);
						
						if(_pro.getLightState() == AlarmProtocol.LIGHT_STATE_GLITCH
								|| _pro.getLightState() == AlarmProtocol.LIGHT_STATE_GLITCH_FIRE)
							_pro.setLightState(AlarmProtocol.LIGHT_STATE_GLITCH_FIRE);
						else
							_pro.setLightState(AlarmProtocol.LIGHT_STATE_GLITCH);
						
						if(_pro.getVoiceState() == AlarmProtocol.VOICE_STATE_GLITCH
								|| _pro.getVoiceState() == AlarmProtocol.VOICE_STATE_GLITCH_FIRE)
							_pro.setVoiceState(AlarmProtocol.VOICE_STATE_GLITCH_FIRE);
						else
							_pro.setVoiceState(AlarmProtocol.VOICE_STATE_GLITCH);
					}
				
					alarm.setStatus(Alarm.STATUS_ALARMED);
				}
				
				List<Integer> states = new ArrayList<Integer>();
				for(AlarmProtocol pro : protocols.values()){
					states.add(NumberUtils.toInt(pro.getLight()));
					states.add(pro.getLightState());
					states.add(NumberUtils.toInt(pro.getRelay()));
					states.add(pro.getRelayState());
					states.add(NumberUtils.toInt(pro.getRelay1()));
					states.add(pro.getRelay1State());
					states.add(NumberUtils.toInt(pro.getVoice()));
					states.add(pro.getVoiceState());
				}
				
				for(AreaHardwareConfig config : aHards.values()){
					states.add(NumberUtils.toInt(config.getLight()));
					states.add(AlarmProtocol.LIGHT_STATE_OK);
					states.add(NumberUtils.toInt(config.getRelay()));
					states.add(AlarmProtocol.RELAY_STATE_OFF);
					states.add(NumberUtils.toInt(config.getRelay1()));
					states.add(AlarmProtocol.RELAY_STATE_OFF);
					states.add(NumberUtils.toInt(config.getVoice()));
					states.add(AlarmProtocol.VOICE_STATE_OFF);
				}
				
				states.add(new Double(max).intValue());
				
				// call DLL
				callAlarm(states, machine);
				
				// update alarms
				alarmDao.updateAlarms(alarms);
			} catch(Throwable t){
				logger.error("Error in sending alarm DLL >> ", t);
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

	public int getEventTime() {
		return eventTime;
	}

	public void setEventTime(int eventTime) {
		this.eventTime = eventTime;
	}

	public int getLogTime() {
		return logTime;
	}

	public void setLogTime(int logTime) {
		this.logTime = logTime;
	}

	public AlarmDao getAlarmDao() {
		return alarmDao;
	}

	public void setAlarmDao(AlarmDao alarmDao) {
		this.alarmDao = alarmDao;
	}

	public int getEventStartTime() {
		return eventStartTime;
	}

	public void setEventStartTime(int eventStartTime) {
		this.eventStartTime = eventStartTime;
	}

	public int getEventEndTime() {
		return eventEndTime;
	}

	public void setEventEndTime(int eventEndTime) {
		this.eventEndTime = eventEndTime;
	}
}
