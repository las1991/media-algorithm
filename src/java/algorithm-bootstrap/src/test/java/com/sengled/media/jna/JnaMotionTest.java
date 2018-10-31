package com.sengled.media.jna;

import com.alibaba.fastjson.JSONObject;
import com.sengled.media.algorithm.config.MotionSensitivity;
import com.sengled.media.interfaces.DisposeableMemory;
import com.sengled.media.interfaces.YUVImage;
import com.sengled.media.jna.jpg_encoder.Jpg_encoderLibrary;
import com.sengled.media.jna.nal_decoder.Nal_decoderLibrary;
import com.sengled.media.jna.sengled_algorithm_base.Sengled_algorithm_baseLibrary;
import com.sengled.media.jna.sengled_algorithm_base.algorithm_base_result2;
import com.sengled.media.jni.JNIFunction;
import com.sengled.media.objectpool.AlgorithmResultObjectPool;
import com.sengled.mediaworker.algorithm.context.AlgorithmConfigWarpper;
import com.sun.jna.Pointer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * @author las
 * @date 18-10-30
 */
public class JnaMotionTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(JnaMotionTest.class);

    private static Nal_decoderLibrary decoderLibrary;
    private static Sengled_algorithm_baseLibrary algorithmLibrary;
    private static Jpg_encoderLibrary encoderLibrary;

    static {
        try {
            System.setProperty("jna.library.path", "/home/las/IdeaProjects/media-algorithm-v3/src/java/algorithm-assembly/libc");
            System.setProperty("jni.library.path", "/home/las/IdeaProjects/media-algorithm-v3/src/java/algorithm-assembly/libc");
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

            LOGGER.info("init finished");
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            LOGGER.error("JnaInterface init failed. System exit.");
            System.exit(1);
        }
    }

    String jsonConfig;

    List<YUVImage> yuvImages = new ArrayList<>();

    @Before
    public void before() throws IOException {

        byte[] yuv1 = Files.readAllBytes(new File("/home/las/Downloads/9B6C4ABF2E574A9395F9D9336FB051D9.yuv").toPath());
        byte[] yuv2 = Files.readAllBytes(new File("/home/las/Downloads/FE3D905C594249818184D4B562D050A9.yuv").toPath());

        for (int i = 0; i < 15; i++) {
            yuvImages.add(new YUVImage(1080, 720, yuv1));
            yuvImages.add(new YUVImage(1080, 720, yuv2));
        }


        List<AlgorithmConfigWarpper.Data> dataList = new ArrayList<>();
        AlgorithmConfigWarpper.Data d = new AlgorithmConfigWarpper.Data();
        d.setId(1);
        d.setPos("0,0,100,100");

        d = new AlgorithmConfigWarpper.Data();
        d.setId(2);
        d.setPos("0,0,100,100");
        dataList.add(d);
        d = new AlgorithmConfigWarpper.Data();
        d.setId(3);
        d.setPos("0,0,100,100");
        dataList.add(d);

        AlgorithmConfigWarpper.MotionConfig motion = new AlgorithmConfigWarpper.MotionConfig();
        motion.setSensitivity(MotionSensitivity.HIGH);
        motion.setDataList(dataList);

        jsonConfig = JSONObject.toJSONString(motion);
    }

    @Test
    public void testBatchMotion() {
        while (true) {
            testMotion();
        }
    }

    @Test
    public void testMotion() {
        Pointer pointer = algorithmLibrary.create_instance("aaa");
        int length = jsonConfig.getBytes().length;
        final DisposeableMemory algorithm_params = new DisposeableMemory(length);
        try {
            algorithm_params.write(0, jsonConfig.getBytes(), 0, length);
            AlgorithmResultObjectPool.getInstance().function(new Function<algorithm_base_result2, Object>() {
                @Override
                public Object apply(algorithm_base_result2 result) {
                    yuvImages.forEach(yuvImage -> {
                        byte[] yuvData = yuvImage.getYUVData();
                        int yuvDataLength = yuvData.length;
                        final DisposeableMemory yuvDataPointer = new DisposeableMemory(yuvDataLength);
                        try {
                            yuvDataPointer.write(0, yuvData, 0, yuvDataLength);
                            algorithmLibrary.feed2(pointer, yuvDataPointer, yuvImage.getWidth(), yuvImage.getHeight(), algorithm_params, result);
                            if (result.bresult != 0) {
                                LOGGER.info("find motion {}", new String(result.result.getByteArray(0, result.size), "UTF8"));
                            }
                        } catch (Exception e) {
                            LOGGER.error("feed result Encoding " + e.getMessage(), e);
                        } finally {
                            yuvDataPointer.dispose();
                            algorithmLibrary.destroy_result(result);
                        }
                    });
                    return null;
                }
            });

        } finally {
            algorithm_params.dispose();
            algorithmLibrary.delete_instance(pointer);
        }

    }

    @After
    public void destory() {

    }


}
