package com.sengled.media.jni;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Files;

public class JNIGenerator {
    public static void main(String[] args) throws IOException, InterruptedException {
    	
        File file = new File("../../c/log4c");
        file.mkdirs();

        System.setProperty("jni.library.path", file.getAbsolutePath());
        
        // System.load("/home/chenxh/workspace/gitlab.com/media-demo-v3/src/java/demo-jnr/libc/liblog4c.so");
        
        ProcessBuilder builder = new ProcessBuilder();

        //System.out.println(file.getAbsolutePath());
        //System.out.println(JNIFunction.class.getResource("/").getFile());
        //System.out.println(JNIFunction.class.getName());

        builder.command("javah", "-classpath", JNIFunction.class.getResource("/").getFile(), "-d", file.getAbsolutePath(), "-jni",
                JNIFunction.class.getName());

        builder.redirectOutput();
        builder.redirectError();
        builder.start().waitFor();
        
        
        File[] cHeaders =
        file.listFiles(new FilenameFilter() {
            
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".h");
            }
        });
        
        for (File cHeader : cHeaders) {
            byte[] bytes = Files.readAllBytes(cHeader.toPath());
            System.out.println("/* " + cHeader.getAbsolutePath() + "*/");
            System.out.println(new String(bytes));
            System.out.println("/*===================================*/");
        }
        
        long ptr = JNIFunction.getInstance().getLog4CFunction();
        JNIFunction.getInstance().invokeLog4CFunction(ptr, 1, "hello");
        
        
    }
    
}
