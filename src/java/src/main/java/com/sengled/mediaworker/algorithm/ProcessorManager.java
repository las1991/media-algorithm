package com.sengled.mediaworker.algorithm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sengled.mediaworker.algorithm.exception.DecodeException;
import com.sengled.mediaworker.algorithm.pydto.Algorithm;
import com.sengled.mediaworker.algorithm.pydto.YUVImage;


public class ProcessorManager {
	private static final Logger LOGGER = LoggerFactory.getLogger(ProcessorManager.class);
	private static final int PYTHON_PROCESSOR_COUNT = Constants.CPU_CORE_COUNT;
	
	private LinkedBlockingQueue<PythonProcessor> idles;
	private List<PythonProcessor> processorList;
	
	public ProcessorManager(){
		LOGGER.info("ProcessorManager init. processorCount:{}",PYTHON_PROCESSOR_COUNT);
		idles  = new LinkedBlockingQueue<PythonProcessor>(PYTHON_PROCESSOR_COUNT);
		for(int i=0;i<PYTHON_PROCESSOR_COUNT;i++){
			PythonProcessor processor = new PythonProcessor(this);
			idles.add(processor);
		}
		processorList = new ArrayList<>();
		processorList.addAll(idles);

		for (PythonProcessor pythonProcessor : processorList) {
			pythonProcessor.start();
		}
	}
	/**
	 * 解码flv
	 * @param src
	 * @return 
	 * @return
	 * @throws ExecutionException 
	 * @throws InterruptedException 
	 * @throws Exception
	 */
	public  Future<YUVImage> decode(final String token,final byte[] src) throws InterruptedException,DecodeException{
		PythonProcessor processor = null;
		try {
			processor = idles.take();
			return processor.submit(new Operation<YUVImage>(){
				@Override
				public YUVImage apply(Function function) {
					return function.decode(token,src);
				}
			});
		} catch (InterruptedException e1) {
			throw e1;
		}finally{
			if(processor !=null){
				idles.put(processor);	
			}
		}
	}
	
	/**
	 * 创建Motion上下文
	 * @param token
	 * @param jsonCnfig
	 * @return
	 * @throws Exception
	 */
	public StreamingContext newAlgorithmContext(String model,String token,Map<String,Object> configs) throws Exception{
		PythonProcessor processor = selectProcessor();
		return processor.newAlgorithm(model, token,configs);
	}
	public boolean  removeIdleProcessor(PythonProcessor processor){
		return idles.remove(processor);
	}
	public void  addIdleProcessor(PythonProcessor processor){
		if(idles.contains(processor) || null == processor){
			return;
		}
		idles.add(processor);
	}

    public void stop() {
        LOGGER.info("Stop all ProcessorInstance ...");
        for(PythonProcessor processor:processorList){
        	processor.shutdown();
        }
    }
    
    private PythonProcessor selectProcessor(){
    	return  Collections.min(processorList, new Comparator<PythonProcessor>() {
			@Override
			public int compare(PythonProcessor o1,PythonProcessor o2) {
				if(o1.getCurrentContextCount() > o2.getCurrentContextCount()){
					return 1;
				}
				if(o1.getCurrentContextCount() == o2.getCurrentContextCount()){
					return 0;
				}
				return -1;
			}
		});
    }
	public StreamingContext findStreamingContext(String token, String model) {
		for(   PythonProcessor proc :  processorList){
			StreamingContext sc = proc.getStreamingContext(token, model);
			if(sc !=null ){
				return sc;
			}
		}
		return null;
	}

}
