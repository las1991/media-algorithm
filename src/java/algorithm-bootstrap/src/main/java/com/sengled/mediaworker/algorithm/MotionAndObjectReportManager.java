package com.sengled.mediaworker.algorithm;

import java.util.concurrent.TimeUnit;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

public class MotionAndObjectReportManager {
    private static final long MOTION_REPORT_INTERVAL_SECOND = 60;
    private static final long OBJECT_REPORT_INTERVAL_SECOND = 60;
    
    
    private static Cache<String, String> tokenMotion60sFlag = CacheBuilder.newBuilder() //触发一次motion 保存60秒
            .expireAfterWrite(MOTION_REPORT_INTERVAL_SECOND, TimeUnit.SECONDS)
            .initialCapacity(1)
            .build();
    
    private static Cache<String, String> tokenObject60sFlag = CacheBuilder.newBuilder() //触发一次object 保存60秒
            .expireAfterWrite(OBJECT_REPORT_INTERVAL_SECOND, TimeUnit.SECONDS)
            .initialCapacity(1)
            .build();
    
    public static void markMotionEvent(String token,String eventTime){
        tokenMotion60sFlag.put(token, eventTime);
    }
    
    public static boolean isAllowMotionReport(String token){
        if( null == tokenMotion60sFlag.getIfPresent(token)){
            return true;
        }
        return false;
    }
    
    public static String getMotionRportTime(String token){
        return tokenMotion60sFlag.getIfPresent(token);
    }
    
    public static void markObjectEvent(String token,String eventTime){
        tokenObject60sFlag.put(token, eventTime);
    }
    
    public static boolean isAllowObjectReport(String token){
        if( null == tokenObject60sFlag.getIfPresent(token)){
            return true;
        }
        return false;
    }
    
    public static String getObjectRportTime(String token){
        return tokenObject60sFlag.getIfPresent(token);
    }
}
