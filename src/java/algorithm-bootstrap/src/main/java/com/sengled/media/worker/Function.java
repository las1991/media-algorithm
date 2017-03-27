package com.sengled.media.worker;
/**
 * python需要实现的接口
 * @author liwei
 * @Date   2017年3月2日 下午3:23:51 
 * @Desc   
 */
public interface Function {
    byte[] apply(String token,byte[] imageBytes);
    String hello();
}

