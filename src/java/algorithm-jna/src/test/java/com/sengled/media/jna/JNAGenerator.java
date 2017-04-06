package com.sengled.media.jna;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import com.ochafik.lang.jnaerator.JNAerator;
import com.sun.jna.Platform;

public class JNAGenerator {
public static void main(String[] args) throws IOException {
    System.out.println(new File(".").getAbsolutePath());

    JNAerator.main(new String[]{"-h"});
    
    File tmp = new File("tmp");
    if (!tmp.exists()) {
        tmp.mkdirs();
    }
    
    File soFile = new File(tmp, Platform.isLinux() ? "jna.so" : "jna.dll");
    if (!soFile.exists()) {
        soFile.createNewFile();
    }
    
    
    /**
     * 解码库
     */
    JNAerator.main(new String[]{
            soFile.getAbsolutePath(), 
            "../../c/decoder/src/naldecoder.h", 
            "-mode", "Directory", 
            "-runtime", "JNA", 
            "-root", "com.sengled.media.jna", 
            "-o", "src/main/java", 
            "-f"});
    
    /**
     * 图片压缩库
     */
    JNAerator.main(new String[]{
            soFile.getAbsolutePath(), 
            "../../c/encoder/src/yuvencoder.h", 
            "-mode", "Directory", 
            "-runtime", "JNA", 
            "-root", "com.sengled.media.jna", 
            "-o", "src/main/java", 
            "-f"});
    
    
    /**
     * motion 检测算法库
     */
    JNAerator.main(new String[]{
            soFile.getAbsolutePath(), 
            "../../c/motion/include/sengled_algorithm.h", 
            "-mode", "Directory", 
            "-runtime", "JNA", 
            "-root", "com.sengled.media.jna", 
            "-o", "src/main/java", 
            "-f"});
}
}
