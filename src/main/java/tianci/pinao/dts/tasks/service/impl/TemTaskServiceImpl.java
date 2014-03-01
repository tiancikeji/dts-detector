package tianci.pinao.dts.tasks.service.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import tianci.pinao.dts.models.Channels;
import tianci.pinao.dts.models.InnerChannel;
import tianci.pinao.dts.models.Temperature;
import tianci.pinao.dts.sal.TemMonitor;
import tianci.pinao.dts.services.ChannelsService;
import tianci.pinao.dts.tasks.dao.TemDao;
import tianci.pinao.dts.tasks.service.TemTaskService;
import tianci.pinao.dts.util.PinaoConstants;

public class TemTaskServiceImpl implements TemTaskService {

	private final Log logger = LogFactory.getLog(getClass());

	private String path = PinaoConstants.TEM_PATH;
	
	private boolean saveStock;
	
	private boolean saveReferTem;
	
	private ChannelsService channelsService;
	
	// local varaiable
	private Holder holder = new Holder();
	
	private TemMonitor tm;

	private TemDao temDao;
	
	@Override
	public void logTemperature() {
		Timestamp ts = temDao.getTemMaxTime();
		if(ts != null){
			temDao.copyTem(ts);
			temDao.removeTem(ts);
		}
	}
	
    @Override
    public void readTem() {
    	long start = System.currentTimeMillis();
    	try {
	    	File dir = new File(path);
	    	if(!(dir.exists() && dir.isDirectory()))
	    		dir.mkdirs();
	    	Channels channels = getChannels();
	    	if(channels != null && channels.getSwitchcom() != null && channels.getSwitchcom() > 0 
	    			&& channels.getPort() != null && channels.getPort() > 0
	    			&& channels.getChannels() != null && channels.getChannels().size() > 0){
	    		// init ...
	    		if(tm == null){
	    			System.loadLibrary("TemMonitor");
	    			tm = new TemMonitor();
	    			int state = tm.InitDts(channels.getSwitchcom(), channels.getPort());
	    			if(logger.isInfoEnabled())
	    				logger.info("Init Dts returned >> " + state);
	    			final TemMonitor tmp = tm;
	    			Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
	    				@Override
	    				public void run() {
	    					if(tmp != null){
	    						tmp.CloseDevice();
	    						if(logger.isInfoEnabled())
	    							logger.info("CloseDevice Dts");
	    					}
	    				}
	    			}));
	    		}
	    		
	    		for(InnerChannel ic : channels.getChannels()){
			    	// 1. call jni
			    	// get channel from db?
			    	int channel = ic.getChannel();
			    	// get length from db?
			    	int length = ic.getLength();
			    	double[] stock = new double[length];
			    	double[] unstock = new double[length];
			    	double[] referTem = new double[1];
			    	double[] tem = new double[length];
			    	long startSAL = System.currentTimeMillis();
			    	tm.PickData(channel, length, stock, unstock, referTem, tem, 0);
			    	if(logger.isInfoEnabled())
			    		logger.info("termReader used >> " + (System.currentTimeMillis() - startSAL) + " ms.");
			    	
			    	StringBuilder sb = new StringBuilder();
			    	sb.append(channel);
		    		sb.append(PinaoConstants.TEM_DATA_COL_SEP);
		    		for(double tm : tem)
		    			sb.append(tm + PinaoConstants.TEM_DATA_ELEMENT_SEP);
		    		sb.append(PinaoConstants.TEM_DATA_COL_SEP);
		    		if(isSaveStock()){
		    			for(double tm : stock)
		        			sb.append(tm + PinaoConstants.TEM_DATA_ELEMENT_SEP);
		        		sb.append(PinaoConstants.TEM_DATA_COL_SEP);
		        		
		    			for(double tm : unstock)
		        			sb.append(tm + PinaoConstants.TEM_DATA_ELEMENT_SEP);
		    		}
		    		sb.append(PinaoConstants.TEM_DATA_COL_SEP);
		    		
		    		if(isSaveReferTem()){
		    			sb.append(referTem[0]);
		    		}
		    		sb.append(PinaoConstants.TEM_DATA_COL_SEP);
			    	
			    	// 2. save into file-read
			    	long prefix = System.currentTimeMillis();
			    	File file = new File(path + "/" + prefix + PinaoConstants.TEM_READ_SUFFIX);
					FileWriter fw = new FileWriter(file);
					fw.write(sb.toString());
					fw.flush();
					fw.close();
					
			    	// 3. rename file-read to file
					file.renameTo(new File(dir, "" + prefix));
	    		}
	    	}
    	} catch (Throwable e) {
    		e.printStackTrace();
    		if(logger.isErrorEnabled())
    			logger.error("Exception when reading term >> ", e);
    	}
    	if(logger.isInfoEnabled())
    		logger.info("ReadTemTask used >> " + (System.currentTimeMillis() - start) + " ms.");
    }

	@Override
	public void saveTem() {
    	long start = System.currentTimeMillis();
    	File dir = new File(path);
    	if(dir.exists() && dir.isDirectory()){
	    	// 1. read files
    		File[] files = dir.listFiles(new FilenameFilter() {
				@Override
				public boolean accept(File dir, String name) {
					return NumberUtils.isNumber(name);
				}
			});
    		
    		List<File> paths = new ArrayList<File>();
    		if(files != null && files.length > 0){
    			// 2. rename files
	    		for(File file : files){
	    			try{
		    			File path = new File(file.getAbsolutePath() + PinaoConstants.TEM_WRITE_SUFFIX);
		    			file.renameTo(path);
		    			paths.add(path);
	    			} catch(Throwable t){
	    				t.printStackTrace();
	    				if(logger.isErrorEnabled())
	    					logger.error("Exception during rename file >> " + file);
	    			}
	    		}
	    		
	    		if(paths.size() > 0)
	    			for(File path : paths){
	    				try{
		    				final List<Temperature> temps = new ArrayList<Temperature>();
		    				final long date = NumberUtils.toLong(StringUtils.removeEnd(path.getName(), PinaoConstants.TEM_WRITE_SUFFIX));
					    	// 3. read file-data
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
		    							temps.add(tem);
		    						}
		    					}
		    				}
		    				sc.close();
		    				
					    	// 4. save into db
		    				if(temps.size() > 0){
		    					for(final Temperature tm : temps){
		    						temDao.saveTem(tm, date);
		    					}
		    				}
		    				
					    	// 5. delete files
		    				path.delete();
	    				} catch(Throwable t){
		    				t.printStackTrace();
		    				if(logger.isErrorEnabled())
		    					logger.error("Exception during saving db >> " + path);
		    			}
	    			}
    		}
    	}
    	if(logger.isInfoEnabled())
    		logger.info("SaveTemTask used >> " + (System.currentTimeMillis() - start) + " ms.");
    }

	private Channels getChannels() {
		if(holder.channels == null || (System.currentTimeMillis() - holder.time) > 600000){
			holder.channels = channelsService.findChannels();
			holder.time = System.currentTimeMillis();
		}
		return holder.channels;
	}

	@Override
	public Map<Integer, List<String>> getTemsByChannels(Set<Integer> ids) {
		return temDao.getTemsByChannels(ids);
	}

	public boolean isSaveStock() {
		return saveStock;
	}

	public void setSaveStock(boolean saveStock) {
		this.saveStock = saveStock;
	}

	public boolean isSaveReferTem() {
		return saveReferTem;
	}

	public void setSaveReferTem(boolean saveReferTem) {
		this.saveReferTem = saveReferTem;
	}

	public ChannelsService getChannelsService() {
		return channelsService;
	}

	public void setChannelsService(ChannelsService channelsService) {
		this.channelsService = channelsService;
	}

	public TemDao getTemDao() {
		return temDao;
	}

	public void setTemDao(TemDao temDao) {
		this.temDao = temDao;
	}

}

class Holder{
	Channels channels;
	
	long time;
}
