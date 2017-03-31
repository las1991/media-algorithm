package com.sengled.mediaworker.algorithm;

import java.io.File;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sengled.mediaworker.algorithm.exception.StreamingContextInitException;
import com.sengled.mediaworker.algorithm.pydto.Algorithm;

import py4j.DefaultGatewayServerListener;
import py4j.GatewayServer;
import py4j.GatewayServerListener;

public class PythonProcessor{
	private static final Logger LOGGER = LoggerFactory.getLogger(PythonProcessor.class);
	private final static String PROJECT_PATH = System.getenv("SENGLED_APP_HOME");
	private final static String PYTHON_MODULE_PATH = PROJECT_PATH + Constants.FILE_SEPARATOR + "python";
	private final static String PYTHON_C_LIB = PYTHON_MODULE_PATH;
	private final static String PYTHON_LOG_PATH = PYTHON_MODULE_PATH;
	private final static String PYTHON_MODULE_MAIN = PYTHON_MODULE_PATH + Constants.FILE_SEPARATOR + "function.py";
	
	private ConcurrentHashMap<String, StreamingContext> streamingContextMap;

	private ProcessorManager processorManager;
	private ExecutorService  singleThread;
	private GatewayServer gateway;
	private Process pythonProcess;
	private Function func;
	
	

	public PythonProcessor(ProcessorManager processorManager) {
		LOGGER.info("PythonProcessor Construct with GatewayServerListener ...");
		singleThread =  Executors.newSingleThreadExecutor();
		streamingContextMap = new ConcurrentHashMap<>();
		this.processorManager = processorManager;
	}
	
	public void start() {
		streamingContextMap.clear();
		gateway = new GatewayServer(null, 0);
		Thread thread = new Thread(new Runnable() {
			@Override
			public void run() {
				gateway.start();
			}
		});
		thread.setDaemon(true);
		thread.start();
		try {
			thread.join();
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}

		int javaPort = gateway.getListeningPort();
		ProcessBuilder builder = new ProcessBuilder("python", PYTHON_MODULE_MAIN, "" + javaPort);
		Map<String, String> evn = builder.environment();
		evn.put("PYTHON_C_LIB", PYTHON_C_LIB);
		File log = new File(PYTHON_LOG_PATH + "/output.log");
		builder.redirectErrorStream(true);
		builder.redirectOutput(Redirect.appendTo(log));
		LOGGER.info("pythonMain:{} javaPort:{} ", PYTHON_MODULE_MAIN, javaPort);
		try {
			pythonProcess = builder.start();
			func = (Function) gateway.getPythonServerEntryPoint(new Class[] { Function.class });
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		
	}

	/**
	 * 提交operation
	 * 
	 * @param operation
	 * @return
	 */
	public <T> Future<T> submit(final Operation<T> operation) {
		final PythonProcessor process = this;		
		return singleThread.submit(new Callable<T>() {	
			@Override
			public T call() throws Exception {
				boolean removed = processorManager.removeIdleProcessor(process);
				try {
					return  operation.apply(func);
				} finally {
					if(removed){
						processorManager.addIdleProcessor(process);
					}
				}
			}
		});
	}

	/**
	 * 初始化python进程的算法模型
	 * 
	 * @param token
	 * @return
	 */
	public StreamingContext newAlgorithm(final String model,final String token,final Map<String, Object> parameters) throws StreamingContextInitException{
		Future<String> pythonObjectIdFuture = singleThread.submit(new Callable<String>() {
			@Override
			public String call() throws Exception {
				return func.newAlgorithmModel(model,token);
			}
		});
		String pythonObjectId=null;
		try {
			pythonObjectId = pythonObjectIdFuture.get();
			if( null == pythonObjectId ){
				throw new StreamingContextInitException("StreamingContextInit failed token:["+token+"]");
			}
		} catch (StreamingContextInitException e) {
			throw  e;
		} catch (ExecutionException e) {
			throw new StreamingContextInitException(e.getMessage(), e.getCause());
		} catch (InterruptedException e) {
			throw new StreamingContextInitException(e.getMessage(), e);
		}
		
		Algorithm algorithm = new Algorithm(pythonObjectId, parameters);
		StreamingContext context  =  new StreamingContext(token, model, this, algorithm );
		final StreamingContext oldContext = streamingContextMap.put(token + "_" + model, context);
		
		if(oldContext != null){
			submit(new Operation<Void>() {
				@Override
				public Void apply(Function function) {
					function.close(oldContext.getAlgorithm());
					return null;
				}
			});
		}
		
		return context;
	}
	/**
	 * 销毁python进程的算法模型
	 * 
	 * @param algorithm
	 */
	public void removeAlgorithm(final StreamingContext streamingContext) {
		//TODO
	}
	public void shutdown() {
		try {
			if (gateway != null) {
				gateway.shutdown();
			}
		} catch (Exception e) {
			LOGGER.error("gateway shutdown failed." + e.getMessage(), e);
		} finally {
			if (pythonProcess != null) {
				pythonProcess.destroy();
			}
		}
	}
	public int getCurrentContextCount(){
		return streamingContextMap.size();
	}
	
	public StreamingContext getStreamingContext(String token,String model){
		return streamingContextMap.get(token + "_" + model);
	}
	
}
