package com.sengled.mediaworker.algorithm;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.sengled.media.interfaces.Algorithm;
import com.sengled.media.interfaces.JnaInterface;
import com.sengled.media.interfaces.YUVImage;
import com.sengled.media.interfaces.exceptions.AlgorithmIntanceCloseException;
import com.sengled.media.interfaces.exceptions.AlgorithmIntanceCreateException;
import com.sengled.media.interfaces.exceptions.DecodeException;
import com.sengled.media.interfaces.exceptions.EncodeException;
import com.sengled.media.interfaces.exceptions.FeedException;
import com.sengled.mediaworker.RecordCounter;
import com.sengled.mediaworker.algorithm.context.StreamingContext;
import com.sengled.mediaworker.algorithm.context.StreamingContextManager;
import com.sengled.mediaworker.algorithm.decode.KinesisFrameDecoder.Frame;
import com.sengled.mediaworker.algorithm.decode.KinesisFrameDecoder.FrameConfig;
import com.sengled.mediaworker.algorithm.feedlistener.FeedListener;

@Component
public class ProcessorManagerImpl implements InitializingBean,ProcessorManager{
	private static final Logger LOGGER = LoggerFactory.getLogger(ProcessorManagerImpl.class);
	
    @Value("${motion.interval.time.msce}")
    private Long motionIntervalTimeMsce;
    
    @Value("${max.delayed.time.msce}")
    private Long maxDelayedTimeMsce;
	
	private JnaInterface jnaInterface;
	private ExecutorService  threadPool;
	
	@Autowired
	private FeedListener[] feedListeners;
	@Autowired
    private StreamingContextManager streamingContextManager;
	@Autowired
	private RecordCounter recordCounter;
	
	@Override
	public void afterPropertiesSet() throws Exception {
		try {
			jnaInterface = new JnaInterface();
			threadPool = Executors.newWorkStealingPool(Math.max(1, Runtime.getRuntime().availableProcessors() - 1));
		} catch (Exception e) {
			LOGGER.error(e.getMessage(),e);
			System.exit(1);
		}		
	}

	public synchronized Future<?> submit(long receiveTime,String tokenMask, Collection<Frame> datas) {
		return threadPool.submit(new Callable<Void>() {
			@Override
			public Void call() throws Exception {
				handleDatas(receiveTime,tokenMask, datas);
				return null;
			}
		});
	}
	
	private  void handleDatas(final long receiveTime,final String tokenMask,final Collection<Frame> datas){
		
		LOGGER.debug("Token:{},handleDatas begin. datas size:{} ",tokenMask,datas.size());
		
		for (Frame frame : datas) {
			long handleStartTime = System.currentTimeMillis();
			long waitProcessCost = handleStartTime - receiveTime;
			recordCounter.updateWaitProcessCost(waitProcessCost);
			LOGGER.debug("Token:{},waitProcessCost:{}",tokenMask,waitProcessCost);
			
			actionHandle(tokenMask, frame);
			
			long processCost = System.currentTimeMillis() -  handleStartTime;
			recordCounter.updateSingleDataProcessCost(processCost);
			LOGGER.debug("Token:{} process cost:{}",tokenMask,processCost);
		}
	}
	
	private void actionHandle(String tokenMask,Frame frame) {
		FrameConfig frameConfig = frame.getConfig();
		String utcDateTime = frameConfig.getUtcDateTime();
		String action = frameConfig.getAction();
		
	    //获得上下文
        StreamingContext context;
        try {
            context = streamingContextManager.findOrCreateStreamingContext(this, tokenMask, utcDateTime,frameConfig);
        } catch (Exception e) {
            LOGGER.error("findOrCreateStreamingContext failed."+e.getMessage(),e);
            return;
        }
        
	    //处理逻辑
        switch (action) {
            case "open":
                context.setAction(context.openAction);
                break;
            case "exec":
                context.setAction(context.execAction);
                break;
            case "close":
                context.setAction(context.closeAction);
                break;
            default:
                LOGGER.error("Token:{},action:{} not supported", tokenMask,action);
                return;
        }
		LOGGER.debug("Token:{},Received config.[ action:{},utcDateTime:{},modelConfig:{} ]",tokenMask,action,utcDateTime,frameConfig);
		

		//过滤数据
		try {
			if(context.isDataExpire(maxDelayedTimeMsce)){
					return;
			}
			context.reportCheck(motionIntervalTimeMsce);
		} catch (Exception e2) {
			LOGGER.error("Token:{}  skip...",tokenMask);
			LOGGER.error(e2.getMessage(),e2);
			return;
		}				

		//执行处理
		try {
			context.feed(frame, feedListeners);
		} catch (Exception e) {
			LOGGER.error(e.getMessage(),e);
			return;
		}
	}

    @Override
	public List<YUVImage> decode(final String token,final byte[] nalData) throws DecodeException{
		return jnaInterface.decode(token, nalData);
	}
	
	public String newAlgorithmModel(String token) throws AlgorithmIntanceCreateException{
		return jnaInterface.newAlgorithmModel(token);
	}
	@Override
	public String feed(Algorithm algorithm, YUVImage yuvImage) throws FeedException {
		return jnaInterface.feed(algorithm.getParametersJson(), algorithm.getAlgorithmModelId(), yuvImage);
	}

	@Override
	public byte[] encode(String token,byte[] yuvData,int width,int  height,int  dstWidth,int  dstHeight ) throws EncodeException{
		return jnaInterface.encode(token, width, height, dstWidth, dstHeight, yuvData);
	}
	@Override
	public void close(String algorithmModelId) throws AlgorithmIntanceCloseException {
		jnaInterface.close(algorithmModelId);
		
	}
	@Override
	public void shutdown(){
		threadPool.shutdownNow();
	}
}
