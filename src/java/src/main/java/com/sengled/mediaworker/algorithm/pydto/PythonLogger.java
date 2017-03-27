package com.sengled.mediaworker.algorithm.pydto;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
/**
 * 用于python进程写日志
 * @author liwei
 * @Date   2017年3月3日 下午4:23:00 
 * @Desc
 */
public class PythonLogger{
    public static final Logger LOGGER = LoggerFactory.getLogger(PythonLogger.class);
   
    //info

    public static void info(String msg){
    	LOGGER.info(msg);
    }
    public static void info(Marker marker,String msg){
    	LOGGER.info(marker, msg);
    }
    public static void info(String format, Object... arguments){
    	LOGGER.info(format, arguments);
    }

    //--warn
    public static void warn(String msg){
    	LOGGER.warn(msg);
    }
    public static void warn(Marker marker,String msg){
    	LOGGER.warn(marker, msg);
    }
    public static void warn(String format, Object... arguments){
    	LOGGER.warn(format, arguments);
    }
    //--error
    public static void error(String msg){
    	LOGGER.error(msg);
    }
    public static void error(Marker marker,String msg){
    	LOGGER.warn(marker, msg);
    }
    public static void error(String format, Object... arguments){
    	LOGGER.warn(format, arguments);
    }
}

