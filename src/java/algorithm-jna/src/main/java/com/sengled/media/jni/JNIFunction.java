package com.sengled.media.jni;

import java.io.File;
import java.io.FilenameFilter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JNIFunction {
    static final Logger LOGGER = LoggerFactory.getLogger(JNIFunction.class);
    static final class JNIFunctionHolder {
        private static final JNIFunction INSTANCE = new JNIFunction();
    }
    
    public static JNIFunction getInstance() {
        return JNIFunctionHolder.INSTANCE;
    }
    
    private JNIFunction (){

        boolean load = false;
        String path = System.getProperty("jni.library.path");
        LOGGER.info("jni.library.path={}", path);
        if (null != path) {
            File dir = new File(path);
            
            File[] files = 
            dir.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.startsWith("liblog4c");
                }
            });
            
            // 加載
            for (File file : files) {
                try{
                   System.load(file.getAbsolutePath());
                   load = true;
                    
                    JNIFunction.LOGGER.info("load {}", file.getAbsolutePath());
                } catch (Exception e) {
                    JNIFunction.LOGGER.debug("{}", file.getAbsolutePath(), e);
                }
            }
        }
        
        if (!load) {
            System.loadLibrary("log4c");
        }
    };
    
    public static void log(int level, String chars) {
    	int lastCharIndex = chars.length() - 1;
		if (chars.length() > 0 && '\n' == chars.charAt(lastCharIndex)) {
    		LOGGER.info("[{}] {}.(\\n)", level, chars.substring(0, lastCharIndex - 1));
    	} else {
    		LOGGER.info("[{}] {}.", level, chars);
    	}
    }

    // public native void log2(int level, String chars);
    
    public native long getLog4CFunction();
    
    public native int invokeLog4CFunction(long funcPtr, int level, String hello);
}