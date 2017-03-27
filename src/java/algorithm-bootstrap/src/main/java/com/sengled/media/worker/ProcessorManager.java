package com.sengled.media.worker;

import java.io.File;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;

import py4j.GatewayServer;
import py4j.GatewayServerListener;

/**
 * python进程管理器
 * 
 * @author liwei
 * @Date 2017年3月1日 下午7:22:41
 * @Desc
 */
public class ProcessorManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessorManager.class);

    private final static int CPU_CORE_COUNT = Runtime.getRuntime().availableProcessors();
    private final static String PROJECT_PATH = System.getenv("SENGLED_APP_HOME");
    private final static String PYTHON_MAIN = PROJECT_PATH + "/python/function.py";
    private final static String PYTHON_LOG_PATH = PROJECT_PATH + "/python";
    private final static String PYTHON_C_LIB = PROJECT_PATH + "/python";
    private final static String METRICS_NAME = "screenshot";
    
    private LinkedBlockingQueue<ProcessorInstance> processors;
    private LinkedBlockingQueue<ProcessorInstance> exceptionProcessors;
    private ThreadPoolExecutor executor;
    private FunctionListener s3FunctionListener;
    private Timer timer;
    private AtomicLong okCount = new AtomicLong();
    private AtomicLong errorCount = new AtomicLong();
    
    private MetricRegistry metricRegistry;
    /**
     * 初始化进程管理器
     * 
     * @param s3FunctionListener
     */
    public ProcessorManager() {
        long threadkeepAliveTime = 3600L;
        executor = new ThreadPoolExecutor(CPU_CORE_COUNT,
                                          CPU_CORE_COUNT,
                                          threadkeepAliveTime, TimeUnit.SECONDS,
                                          new ArrayBlockingQueue<Runnable>(CPU_CORE_COUNT * 2), 
                                          new ThreadPoolExecutor.CallerRunsPolicy());
        processors = new LinkedBlockingQueue<ProcessorInstance>(CPU_CORE_COUNT);
        exceptionProcessors = new LinkedBlockingQueue<ProcessorInstance>(CPU_CORE_COUNT);
        
        LOGGER.info("ProcessorManager init ...cpuCoreCount:{}", CPU_CORE_COUNT);
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                exceptionProcessorsCheck();
            }
        }, 10000,5000);
       
    }

    /**
     * 设置回调类
     * 
     * @param demoFunctionListener
     */
    public void setFunctionListener(FunctionListener s3FunctionListener) {
        this.s3FunctionListener = s3FunctionListener;
    }
    /**
     * 设置监控类
     * @param metricRegistry
     */
    public void registerMetricRegistry(MetricRegistry metricRegistry) {
        this.metricRegistry = metricRegistry;
        this.metricRegistry.register( MetricRegistry.name(METRICS_NAME, "okCount"), new Gauge<Long>(){
            @Override
            public Long getValue() {
                return okCount.getAndSet(0);
            }
        });
        this.metricRegistry.register( MetricRegistry.name(METRICS_NAME, "errorCount"), new Gauge<Long>(){
            @Override
            public Long getValue() {
                return errorCount.getAndSet(0);
            }
        });
    }

    /**
     * 根据CPU内核数启动同等数量的python子进程
     * 
     * @throws IOException
     */
    public void runPython() {
        LOGGER.info("Run python pythonMain:{}", PYTHON_MAIN);
        LOGGER.info("cpuCoreCount:{}", CPU_CORE_COUNT);

        for (int i = 1; i <= CPU_CORE_COUNT; i++) {
            try {
                processors.put(ProcessorInstance.newInstance());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * 将Kinesis Record.提交到线程池中，申请一个python子进程处理
     * 
     * @param record
     * @return
     */
    public Future<byte[]> submit(final String token, final byte[] imageBytes) {
        Future<byte[]> result = executor.submit(new Callable<byte[]>() {
            @Override
            public byte[] call() throws Exception {
                ProcessorInstance processor = processors.take();
                try {
                    Function function = processor.getFunction();
                    byte[] callResult = function.apply(token, imageBytes);
                    processors.put(processor);
                    okCount.incrementAndGet();
                    return  callResult;
                } catch (Exception e) {
                    LOGGER.error(" Exception:" + e.getMessage(), e);
                    errorCount.incrementAndGet();
                    exceptionProcessors.put(processor);
                }
                return null;
            }
        });
        byte[] resultData = null;
        Exception handleException = null;
        try {
            resultData = result.get();
        } catch (Exception e) {
            LOGGER.error("Exception:" + e.getMessage());
            handleException = e;
        } finally {
            if (resultData == null) {
                LOGGER.error("resultData is null");
            }
            if (s3FunctionListener == null) {
                LOGGER.error("FunctionListener is null");
            }
            if (s3FunctionListener != null && resultData != null) {
                s3FunctionListener.operationComplete(handleException, token, imageBytes, resultData);
            }
        }
        return result;
    }
    /**
     * 调用hello() 如返回失败，则重启python进程 
     */
    public void exceptionProcessorsCheck(){
        LOGGER.info("check exceptionProcessors list");
        while(exceptionProcessors.size()>0){
            ProcessorInstance instance = null;
            boolean isError = false;
            String callResult;
            try {
                instance = exceptionProcessors.take();
                callResult = instance.getFunction().hello();
                if( "hello".equals(callResult)){
                   LOGGER.info("check instance port:{} is ok",instance.getGatewayServer().getListeningPort());
                   processors.put(instance);
                }else{
                    LOGGER.info("CALL hello() return:{}", callResult);
                    isError = true;
                }
            } catch (Exception e) {
                isError = true;
                LOGGER.error("CALL hello() Exception "+e.getMessage(),e);
            }
            
            if(isError){
                try {
                    LOGGER.info("instance:{} shutdown..",instance);
                    instance.shutdown();
                    LOGGER.info("ProcessorInstance newInstance.");
                    instance = ProcessorInstance.newInstance();
                    exceptionProcessors.remove(instance);
                    processors.put(instance);
                } catch (InterruptedException e) {
                    LOGGER.error("check failed! "+e.getMessage(),e);
                }
            } 
        }
    }
    
    public void destroyAll() {
        try {
            LOGGER.info("stop check exceptionProcessors...");
            timer.cancel();
            LOGGER.info("ThreadPoolExecutor shutdown...");
            executor.shutdown();
            executor.awaitTermination(10, TimeUnit.SECONDS);
            
        } catch (Exception e) {
           LOGGER.error(e.getMessage(),e);
        }
        LOGGER.info("ProcessorInstance shutdown...");
        for(ProcessorInstance instance : processors){
            instance.shutdown();
        }
        for(ProcessorInstance instance : exceptionProcessors){
            instance.shutdown();
        }
    }

    public final static class ProcessorInstance {
        private GatewayServer gatewayServer;
        private GatewayServerListener listener;
        private Process process;
        private Function function;
        private Integer javaPort;

        public ProcessorInstance(GatewayServer gatewayServer, GatewayServerListener listener, Process process) {
            this.gatewayServer = gatewayServer;
            this.listener = listener;
            this.process = process;
        }

        public void shutdown() {
            try {
                gatewayServer.shutdown();
            } finally {
                process.destroy();
            }
        }

        public static ProcessorInstance newInstance() {
            final GatewayServer gatewayServer = new GatewayServer(null, 0);
            Py4jGatewayServerListener py4jListener = new Py4jGatewayServerListener(gatewayServer);
            gatewayServer.addListener(py4jListener);
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    gatewayServer.start();
                }
            });
            // thread.setName("py4j-gateway-init");
            thread.setDaemon(true);
            thread.start();
            // 等待线程执行
            try {
                thread.join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            int javaPort = gatewayServer.getListeningPort();
            ProcessBuilder builder = new ProcessBuilder("python", PYTHON_MAIN, "" + javaPort);
            Map<String, String> evn = builder.environment();
            evn.put("PYTHON_C_LIB", PYTHON_C_LIB);
            File log = new File(PYTHON_LOG_PATH + "/output.log");
            builder.redirectErrorStream(true);
            builder.redirectOutput(Redirect.appendTo(log));
            LOGGER.info("pythonMain:{} javaPort:{} ", PYTHON_MAIN, javaPort);
            Process p;
            try {
                p = builder.start();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return new ProcessorInstance(gatewayServer, py4jListener, p);
        }

        public Function getFunction() {
            if(function!=null){
                return function;
            }else{
                this.function = (Function) gatewayServer.getPythonServerEntryPoint(new Class[] { Function.class });
                return function;    
            }
            
        }

        public GatewayServer getGatewayServer() {
            return gatewayServer;
        }

        public void setGatewayServer(GatewayServer gatewayServer) {
            this.gatewayServer = gatewayServer;
        }

        public GatewayServerListener getListener() {
            return listener;
        }

        public void setListener(GatewayServerListener listener) {
            this.listener = listener;
        }

        public Process getProcess() {
            return process;
        }

        public void setProcess(Process process) {
            this.process = process;
        }

        public Integer getJavaPort() {
            if(javaPort == null){
                javaPort = gatewayServer.getListeningPort();
            }
            return javaPort;
        }
        public void setFunction(Function function) {
            this.function = function;
        }
        
    }
}
