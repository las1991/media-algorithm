package com.sengled.mediaworker.algorithm;

import java.io.File;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sengled.mediaworker.algorithm.pydto.Algorithm;

import py4j.DefaultGatewayServerListener;
import py4j.GatewayServer;
import py4j.GatewayServerListener;

public class PythonProcessor implements Comparable<PythonProcessor>{
	private static final Logger LOGGER = LoggerFactory.getLogger(PythonProcessor.class);
	private final static String PROJECT_PATH = System.getenv("SENGLED_APP_HOME");
	private final static String PYTHON_MODULE_PATH = PROJECT_PATH + Constants.FILE_SEPARATOR + "python";
	private final static String PYTHON_C_LIB = PYTHON_MODULE_PATH;
	private final static String PYTHON_LOG_PATH = PYTHON_MODULE_PATH;
	private final static String PYTHON_MODULE_MAIN = PYTHON_MODULE_PATH + Constants.FILE_SEPARATOR + "function.py";
	private final static int TASK_QUEUE_SIZE = 10;

	/**
	 * 需要构造的成员
	 */
	private ThreadPoolExecutor singleThread;
	private BlockingQueue<Runnable> taskQueue;
	private GatewayServerListener gatewayServerListener;

	private GatewayServer gateway;
	private Function func;
	private Process pythonProcess;

	public PythonProcessor() {
		LOGGER.info("PythonProcessor Construct...");
		gatewayServerListener = new DefaultGatewayServerListener();
		initThread();
	}

	public PythonProcessor(GatewayServerListener listener) {
		LOGGER.info("PythonProcessor Construct with GatewayServerListener ...");
		gatewayServerListener = listener;
		initThread();
	}

	private void initThread() {
		this.taskQueue = new ArrayBlockingQueue<Runnable>(TASK_QUEUE_SIZE);
		singleThread = new ThreadPoolExecutor(1, 1, 60, TimeUnit.SECONDS, taskQueue,
				new ThreadPoolExecutor.CallerRunsPolicy());


	}

	public void startup() {
		gateway = new GatewayServer(null, 0);
		gateway.addListener(gatewayServerListener);
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
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		func = (Function) gateway.getPythonServerEntryPoint(new Class[] { Function.class });
	}

	/**
	 * 提交operation
	 * 
	 * @param operation
	 * @return
	 */
	public <T> Future<T> submit(final Operation<T> operation) {
		// final PythonProcessor processor = this;
		return singleThread.submit(new Callable<T>() {// 单线程执行，保证顺序调用processor
			@Override
			public T call() throws Exception {
				// processorManager.removeProcessor(processor);
				return operation.apply(func);
				// processorManager.addProcessor(processor);
			}
		});
	}

	/**
	 * 初始化python进程的算法模型
	 * 
	 * @param token
	 * @return
	 */
	public String newAlgorithm(String model,String token) throws Exception {
		try {
			return func.newAlgorithmModel(model,token);
		} catch (Exception e) {
			LOGGER.error("Call newAlgorithm failed.",e);
			throw e;
		}
	}

	/**
	 * 销毁python进程的算法模型
	 * 
	 * @param algorithm
	 */
	public void removeAlgorithm(Algorithm algorithm) {
		try {
			func.close(algorithm);
		} catch (Exception e) {
			LOGGER.error("RemoveAlgorithm failed." + e.getMessage(), e);
		}
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

	public Integer getTaskQueueCount() {
		return taskQueue.size();
	}
	public void hello(){
		func.hello();
	}

	@Override
	public int compareTo(PythonProcessor o) {
		if(o.getTaskQueueCount()> this.getTaskQueueCount()){
			return -1;
		}
		if(o.getTaskQueueCount()== this.getTaskQueueCount()){
			return 0;
		}
		return 1;
	}
}
