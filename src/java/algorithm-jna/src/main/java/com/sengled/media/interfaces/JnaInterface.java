package com.sengled.media.interfaces;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.sengled.media.interfaces.exceptions.AlgorithmIntanceCloseException;
import com.sengled.media.interfaces.exceptions.AlgorithmIntanceCreateException;
import com.sengled.media.interfaces.exceptions.DecodeException;
import com.sengled.media.interfaces.exceptions.EncodeException;
import com.sengled.media.interfaces.exceptions.FeedException;
import com.sengled.media.jna.jpg_encoder.JPGFrame;
import com.sengled.media.jna.jpg_encoder.Jpg_encoderLibrary;
import com.sengled.media.jna.nal_decoder.Nal_decoderLibrary;
import com.sengled.media.jna.nal_decoder.YUVFrame2;
import com.sengled.media.jna.sengled_algorithm_base.Sengled_algorithm_baseLibrary;
import com.sengled.media.jna.sengled_algorithm_base.algorithm_base_result2;
import com.sengled.media.jni.JNIFunction;
import com.sengled.media.objectpool.AlgorithmResultObjectPool;
import com.sengled.media.objectpool.DecodeYUVFrame2ObjectPool;
import com.sengled.media.objectpool.EncodeJPGFrameObjectPool;
import com.sun.jna.Pointer;

public class JnaInterface implements CFunction {
    private static final Logger LOGGER = LoggerFactory.getLogger(JnaInterface.class);

    private static Nal_decoderLibrary decoderLibrary;
    private static Sengled_algorithm_baseLibrary algorithmLibrary;
    private static Jpg_encoderLibrary encoderLibrary;
    private static AlgorithmResultObjectPool algorithmResultObjectPool;
    private static EncodeJPGFrameObjectPool encodeJPGFrameObjectPool;
    private static DecodeYUVFrame2ObjectPool decodeYUVFrame2ObjectPool;
    
    private ConcurrentHashMap<String, Pointer> pointerMap = new ConcurrentHashMap<>();

    static {
        try {
            LOGGER.info("init...");
            String jnaHome = System.getProperty("jna.library.path");
            LOGGER.info("jna.library.path={}", jnaHome);

            decoderLibrary = Nal_decoderLibrary.INSTANCE;
            decoderLibrary.Init();
            decoderLibrary.SetLogCallback(new Pointer(JNIFunction.getInstance().getLog4CFunction()));

            algorithmLibrary = Sengled_algorithm_baseLibrary.INSTANCE;
            algorithmLibrary.SetLogCallback(new Pointer(JNIFunction.getInstance().getLog4CFunction()));

            encoderLibrary = Jpg_encoderLibrary.INSTANCE;
            encoderLibrary.Init();
            encoderLibrary.SetLogCallback(new Pointer(JNIFunction.getInstance().getLog4CFunction()));

            algorithmResultObjectPool = AlgorithmResultObjectPool.getInstance();

            encodeJPGFrameObjectPool = EncodeJPGFrameObjectPool.getInstance();
            
            decodeYUVFrame2ObjectPool = new DecodeYUVFrame2ObjectPool();
            LOGGER.info("init finished");
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            LOGGER.error("JnaInterface init failed. System exit.");
            System.exit(1);
        }
    }


    @Override
    public List<YUVImage> decode(String token, byte[] nalData) throws DecodeException {
        if (null == nalData || nalData.length == 0) {
            LOGGER.error("Token:{} decode params error.", token);
            throw new IllegalArgumentException("decode params error.");
        }
        LOGGER.debug("decode token:{},nalData length:{}", token, nalData.length);

        List<YUVImage> yuvImageList = new ArrayList<>();
        final ByteBuffer data_buffer = ByteBuffer.wrap(nalData);
        final int len = nalData.length;
        
        return  decodeYUVFrame2ObjectPool.function(new Function<YUVFrame2, List<YUVImage>>() {
                @Override
                public List<YUVImage> apply(YUVFrame2 yuv_frame) {
                    try {
                        int code = decoderLibrary.DecodeNal(data_buffer, len, token, yuv_frame);
                        if (0 != code) {
                            LOGGER.error("decode failed. code:{} token:{}", code, token);
                            throw new Exception("return code error.");
                        }
                        int size0 = yuv_frame.size[0];
                        int size1 = yuv_frame.size[1];
                        if (size0 != 0) {
                            byte[] yuvData = yuv_frame.data[0].getByteArray(0, yuv_frame.size[0]);
                            if (null == yuvData || 0 == yuvData.length) {
                                LOGGER.error("decode failed. yuvData empty. code:{} token:{}", code, token);
                                throw new Exception("yuvData empty.");
                            }
                            yuvImageList.add(new YUVImage(yuv_frame.width, yuv_frame.height, yuvData));
    
                        }
                        if (size1 != 0) {
                            byte[] yuvData = yuv_frame.data[1].getByteArray(0, yuv_frame.size[1]);
                            if (null == yuvData || 0 == yuvData.length) {
                                LOGGER.error("decode failed. yuvData empty. code:{} token:{}", code, token);
                                throw new Exception("yuvData empty.");
                            }
                            yuvImageList.add(new YUVImage(yuv_frame.width, yuv_frame.height, yuvData));
                        }
                        LOGGER.debug("Token:{},decode finished. width:{},height:{}", token, yuv_frame.width, yuv_frame.height);
                    } catch (Exception e) {
                        LOGGER.error(e.getMessage(),e);
                    } finally {
                        decoderLibrary.Destroy(yuv_frame);
                        LOGGER.debug("Destroy yuv_frame");
                    }
                    return yuvImageList;
                }});
    }

    @Override
    public byte[] encode(String token, int width, int height, int dstWidth, int dstHeight, byte[] yuvData) throws EncodeException {
        int yuvDataLength = yuvData.length;
        if (null == yuvData || 0 == yuvDataLength) {
            LOGGER.error("Token:{} encode params error.", token);
            throw new EncodeException("encode params error.");
        }

        LOGGER.debug("Token:{} encode ,yuvData length:{}", token, yuvData.length);
        
        final DisposeableMemory pointer = new DisposeableMemory(yuvDataLength);
        pointer.write(0, yuvData, 0, yuvDataLength);
        com.sengled.media.jna.jpg_encoder.YUVFrame yuv_frame = new com.sengled.media.jna.jpg_encoder.YUVFrame(width, height, pointer, yuvDataLength);

        try {
            return encodeJPGFrameObjectPool.function(new Function<JPGFrame, byte[]>() {
                @Override
                public byte[]  apply(JPGFrame jpg_frame) {
                    try {
                        int code = encoderLibrary.EncodeJPG(yuv_frame, dstWidth, dstHeight, token, jpg_frame);
                        if (0 != code) {
                            LOGGER.error("Token:{} encode failed. code:{}", token, code);
                            throw new Exception("return code error.");
                        }
                        return jpg_frame.data.getByteArray(0, jpg_frame.size);
                    } catch (Exception e) {
                        LOGGER.error(e.getMessage(),e);
                    } finally {
                        encoderLibrary.Destroy(jpg_frame);
                    }
                    return null;
                }});
        } catch (Exception e) {
            LOGGER.error(e.getMessage(),e);
            throw new EncodeException("EncodeException");
        }finally{
            pointer.dispose();
        }
    }

    @Override
    public String newAlgorithmModel(String token) throws AlgorithmIntanceCreateException {
        LOGGER.debug("Token:{}  newAlgorithmModel", token);
        String algorithmModelId;
        Pointer oldPointer = null;
        try {
            Pointer pointer = algorithmLibrary.create_instance(token);
            algorithmModelId = UUID.randomUUID().toString();
            oldPointer = pointerMap.put(algorithmModelId, pointer);
        } catch (Exception e) {
            throw new AlgorithmIntanceCreateException(e);
        } finally {
            try {
                if (null != oldPointer) {
                    algorithmLibrary.delete_instance(oldPointer);
                }
            } catch (Exception e) {
                LOGGER.warn("delete oldPointer error.", e);
            }
        }



        return algorithmModelId;
    }

    @Override
    public String feed(String jsonConfig, String algorithmModelId, YUVImage yuvImage) throws FeedException {
        LOGGER.debug("feed AlgorithmModelId:{},jsonConfig:{} ", algorithmModelId, jsonConfig);
        if (null == jsonConfig || null == yuvImage || null == algorithmModelId) {
            LOGGER.error("jsonConfig:{},cObjectID:{},yuvImage:{}", jsonConfig, algorithmModelId, yuvImage);
            throw new IllegalArgumentException("feed params exception.");
        }

        Pointer algorithmModelPointer = pointerMap.get(algorithmModelId);
        if (null == algorithmModelPointer) {
            LOGGER.info("jsonConfig:{},algorithmModelId:{}", jsonConfig, algorithmModelId);
            throw new FeedException("Not fonud algorithmModelPointer from pointerMap");
        }

        byte[] yuvData = yuvImage.getYUVData();
        int yuvDataLength = yuvData.length;
        if (0 == yuvDataLength) {
            throw new FeedException("yuvDataLength is empay");
        }

        int length;
        final DisposeableMemory algorithm_params;
        final DisposeableMemory yuvDataPointer;
        try {
            length = jsonConfig.getBytes("utf-8").length;
            algorithm_params = new DisposeableMemory(length);
            algorithm_params.write(0, jsonConfig.getBytes("utf-8"), 0, length);

            yuvDataPointer = new DisposeableMemory(yuvDataLength);
            yuvDataPointer.write(0, yuvData, 0, yuvDataLength);
        } catch (Exception e1) {
            throw new FeedException(e1);
        }

        LOGGER.debug("yuvDataPointer data length:{}", yuvDataPointer.getByteArray(0, yuvDataLength).length);

        try {
            return algorithmResultObjectPool.function(new Function<algorithm_base_result2, String>() {
                @Override
                public String apply(algorithm_base_result2 result) {
                    try {
                        algorithmLibrary.feed2(algorithmModelPointer, yuvDataPointer, yuvImage.getWidth(), yuvImage.getHeight(), algorithm_params,result);
                        if (result.bresult != 0) {
                            return new String(result.result.getByteArray(0, result.size), "UTF8");
                        }
                    } catch (Exception e) {
                        LOGGER.error("feed result Encoding " + e.getMessage(), e);
                    }finally{
                        algorithmLibrary.destroy_result(result);
                    }
                    return null;
                }
            });
        } catch (Exception e) {
            throw new FeedException(e);
        } finally {
            yuvDataPointer.dispose();
            algorithm_params.dispose();
        }
    }

    @Override
    public void close(String algorithmModelId) throws AlgorithmIntanceCloseException {
        LOGGER.debug("close algorithmModelId:{}", algorithmModelId);

        LOGGER.debug("pointerMap size:{}", pointerMap.size());

        if (null == algorithmModelId || "".equals(algorithmModelId)) {
            throw new AlgorithmIntanceCloseException("parmas error.");
        }

        try {
            Pointer pointer = pointerMap.remove(algorithmModelId);
            if (null != pointer) {
                algorithmLibrary.delete_instance(pointer);
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            throw new AlgorithmIntanceCloseException(e);
        }
    }
}

