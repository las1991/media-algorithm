package com.sengled.mediaworker.algorithm;

public class Constants {
	//操作系统
	public static final String OS_NAME = System.getProperties().getProperty("os.name");
	//目录分隔符
	public static final String FILE_SEPARATOR = System.getProperties().getProperty("file.separator");
	
	public final static int CPU_CORE_COUNT = Runtime.getRuntime().availableProcessors();

}
