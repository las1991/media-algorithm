package com.sengled.mediaworker.algorithm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sengled.mediaworker.algorithm.pydto.Algorithm;
import com.sengled.mediaworker.algorithm.pydto.YUVImage;


public class ProcessorManager {
	private static final Logger LOGGER = LoggerFactory.getLogger(ProcessorManager.class);
	private static final int PYTHON_PROCESSOR_COUNT = Constants.CPU_CORE_COUNT;
	
	private List<PythonProcessor> processorList;
	private Timer timer = new Timer();
	public ProcessorManager(){
		init(PYTHON_PROCESSOR_COUNT);
	}
	public ProcessorManager(int pythonProcessorCount){
		init(pythonProcessorCount);
	}
	
	public void init(int pythonProcessorCount){
		LOGGER.info("ProcessorManager init. processorCount:{}",pythonProcessorCount);
		processorList  = new ArrayList<PythonProcessor>(pythonProcessorCount);
		for(int i=0;i<pythonProcessorCount;i++){
			PythonProcessor processor = new PythonProcessor();
			processor.startup();
			processorList.add(processor);
		}
		
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				exceptionProcessorsCheck();
			}
		}, 10000, 5000);
	}
	/**
	 * 解码flv
	 * @param src
	 * @return
	 * @throws Exception
	 */
	public YUVImage decode(final String token,final byte[] src) throws Exception{
		PythonProcessor processor = null;
		Future<YUVImage> future = null;
		processor = selectByQueueSize();
		LOGGER.info("select processor end");
		try {
			future = processor.submit(new Operation<YUVImage>(){
				@Override
				public YUVImage apply(Function function) {
					return function.decode(token,src);
				}
			});
		} catch (Exception e) {
			throw e;
		}
		YUVImage image =  future.get();
		if(image == null){
			throw new Exception("Decode failed");
		}
		return image;
	}
	
	/**
	 * 创建Motion上下文
	 * @param token
	 * @param jsonCnfig
	 * @return
	 * @throws Exception
	 */
	public StreamingContext newAlgorithmContext(String model,String token,Map<String,Object> configs) throws Exception{
		PythonProcessor processor =  selectByQueueSize();
		LOGGER.info("select processor end");
		//初始化算法模型
		String pythonObjectId = processor.newAlgorithm(model,token);
		if(StringUtils.isBlank(pythonObjectId)){
			throw new Exception("CALL newAlgorithmContext failed.pythonObjectId is null");
		}
		Algorithm algorithm = new Algorithm(pythonObjectId,configs);
		return new StreamingContext(token,model, processor,algorithm);
	}
	
	/**
	 * 选择一个进程 
	 * @return
	 */
	private PythonProcessor selectByActiveCount() {
		LOGGER.info("Select one  processor");
		return Collections.min(processorList, new Comparator<PythonProcessor>() {
			@Override
			public int compare(PythonProcessor o1, PythonProcessor o2) {
				int q1ActiveCount = o1.getSingleThread().getActiveCount();
				int q2ActiveCount = o2.getSingleThread().getActiveCount();
				if( q1ActiveCount> q2ActiveCount){
					return -1;
				}
				if(q1ActiveCount == q2ActiveCount){
					return 0;
				}
				return 1;
			}
		});
	}
	private PythonProcessor selectByQueueSize() {
		LOGGER.info("Select one  processor");
		return Collections.min(processorList, new Comparator<PythonProcessor>() {
			@Override
			public int compare(PythonProcessor o1, PythonProcessor o2) {
				int q1Size = o1.getSingleThread().getQueue().size();
				int q2Size = o2.getSingleThread().getQueue().size(); 
				if(q1Size  > q2Size ){
					return -1;
				}
				if(q1Size  == q2Size){
					return 0;
				}
				return 1;
			}
		});
	}
	private void exceptionProcessorsCheck() {
		LOGGER.info("run exceptionProcessorsCheck");
		for(PythonProcessor processor :processorList ){
			try {
				processor.hello();
			} catch (Exception e) {
				LOGGER.error("Call Python processor hello() failed.");
				processor.shutdown();
				processor.startup();
			}
		}
	}
    public void destroyAll() {
        try {
            LOGGER.info("stop check exceptionProcessors...");
            timer.cancel();
        } catch (Exception e) {
           LOGGER.error(e.getMessage(),e);
        }
        LOGGER.info("ProcessorInstance shutdown...");
        for(PythonProcessor processor:processorList){
        	processor.shutdown();
        }
        
    }
}
