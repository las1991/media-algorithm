package com.sengled.media.jni;

import java.io.File;
import java.io.FilenameFilter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JNIFunction {
	private static final Logger LOGGER = LoggerFactory.getLogger(JNIFunction.class);
    private static final int LOGLEVEL_INFO = 2;
	private static final int LOGLEVEL_WARN = 3;
    private static final int LOGLEVEL_EORROR = 4;
    private static interface LeveledLogger {
    	public void doLog(String chars);
    }
    
    private static final LeveledLogger[] LOGGERS;
    static {
        
    	// 输出 debug 日志
    	LeveledLogger debugLogger = new LeveledLogger() {
			@Override
			public void doLog(String chars) {
		        final int lastCharIndex = chars.length() - 1;
				if (chars.length() > 0 && '\n' == chars.charAt(lastCharIndex)) {
					LOGGER.debug("{}.\\n", chars.substring(0, lastCharIndex));
				} else {
					LOGGER.debug("{}.", chars);
				}
			}
		};
		
		
    	// 输出 info 日志
    	LeveledLogger infoLogger = new LeveledLogger() {
			@Override
			public void doLog(String chars) {
		        final int lastCharIndex = chars.length() - 1;
				if (chars.length() > 0 && '\n' == chars.charAt(lastCharIndex)) {
					LOGGER.info("{}.\\n", chars.substring(0, lastCharIndex));
				} else {
					LOGGER.info("{}.", chars);
				}
			}
		};
		
		
    	// 输出 warn 日志
    	LeveledLogger warnLogger = new LeveledLogger() {
			@Override
			public void doLog(String chars) {
		        final int lastCharIndex = chars.length() - 1;
				if (chars.length() > 0 && '\n' == chars.charAt(lastCharIndex)) {
					LOGGER.warn("{}.\\n", chars.substring(0, lastCharIndex));
				} else {
					LOGGER.warn("{}.", chars);
				}
			}
		};
		
    	// 输出 error 日志
    	LeveledLogger errorLogger = new LeveledLogger() {
			@Override
			public void doLog(String chars) {
		        final int lastCharIndex = chars.length() - 1;
				if (chars.length() > 0 && '\n' == chars.charAt(lastCharIndex)) {
					LOGGER.error("{}.\\n", chars.substring(0, lastCharIndex));
				} else {
					LOGGER.error("{}.", chars);
				}
			}
		};
		
        // logger 赋值
        LOGGERS = new LeveledLogger[256];
        for (int level = 0; level < LOGGERS.length; level++) {
            if (level < LOGLEVEL_INFO) {
                LOGGERS[level] = debugLogger;
            } else if (level < LOGLEVEL_WARN) {
                LOGGERS[level] = infoLogger;
            } else if (level < LOGLEVEL_EORROR) {
                LOGGERS[level] = warnLogger;
            } else {
                LOGGERS[level] = errorLogger;
            }
        }
    }
    private static final class JNIFunctionHolder {
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
    	if (0 <= level && level < LOGGERS.length) {
    		LOGGERS[level].doLog(chars);
    	} else if(level < 0) {
    		LOGGERS[0].doLog(chars);
    	} else {
    		LOGGERS[LOGGERS.length - 1].doLog(chars);
    	}
    }

    // public native void log2(int level, String chars);
    
    public native long getLog4CFunction();
    
    public native int invokeLog4CFunction(long funcPtr, int level, String hello);
}
