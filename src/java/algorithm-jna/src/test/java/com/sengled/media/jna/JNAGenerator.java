package com.sengled.media.jna;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

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
    
    Files.copy(new File("../../c/decoder/src/nal_decoder.h").toPath(), new File("./nal_decoder.h").toPath(), StandardCopyOption.REPLACE_EXISTING);
    Files.copy(new File("../../c/encoder/src/jpg_encoder.h").toPath(), new File("./jpg_encoder.h").toPath(), StandardCopyOption.REPLACE_EXISTING);
    Files.copy(new File("../../c/motion/include/sengled_algorithm.h").toPath(), new File("./sengled_algorithm.h").toPath(), StandardCopyOption.REPLACE_EXISTING);
    Files.copy(new File("../../c/common/log.h").toPath(), new File("./log.h").toPath(), StandardCopyOption.REPLACE_EXISTING);
    Files.copy(new File("../../c/common/yuv.h").toPath(), new File("./yuv.h").toPath(), StandardCopyOption.REPLACE_EXISTING);
    /**
     * 图片压缩库
     */
    JNAerator.main(new String[]{
            soFile.getAbsolutePath(), 
            "./jpg_encoder.h", 
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
            "./nal_decoder.h", 
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
            "./sengled_algorithm.h", 
            "-mode", "Directory", 
            "-runtime", "JNA", 
            "-root", "com.sengled.media.jna", 
            "-o", "src/main/java", 
            "-f"});
    
}
}
