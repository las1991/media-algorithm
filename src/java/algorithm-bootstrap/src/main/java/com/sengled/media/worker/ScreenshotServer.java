package com.sengled.media.worker;
import java.sql.Timestamp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;

import com.sengled.media.worker.s3.S3;
import com.sengled.media.worker.spring.DefaultBootApplication;

/**
 * 截图服务
 * @author liwei
 * @Date   2017年3月2日 下午4:51:24 
 * @Desc
 */
public class ScreenshotServer {
	/** 系统运行时参数，保存 spring 依赖的配置文件 */
	public static final String SPRING_CONFIG_LOCATION = "spring.config.location";
	
	public static void main(String[] args) {
		final long startAt = System.currentTimeMillis();

		// spring 用到的配置文件
		if (null == System.getProperty(SPRING_CONFIG_LOCATION)) {
			throw new IllegalArgumentException(
					"Not Found Env property '-Dspring.config.location', please use -Dspring.config.location=classpath:/config/application.properties,file:/etc/sengled/sengled.properties");
		}
		SpringApplication.run(new Object[] {
		                                    DefaultBootApplication.class,
											ScreenShotKinesisStreamProcessor.class,
											S3.class
								}, args);
		System.out.println("ScreenshotServer v3 started at " + new Timestamp(System.currentTimeMillis()) + ", cost " + (System.currentTimeMillis() - startAt) + "ms");
		
	}
}
