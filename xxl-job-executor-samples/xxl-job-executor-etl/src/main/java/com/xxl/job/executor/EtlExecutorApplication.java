package com.xxl.job.executor;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @author Simon Cao 2026-08-08 11:04:00
 */
@SpringBootApplication
@MapperScan("com.xxl.job.executor.cdc.mapper")
public class EtlExecutorApplication {

	public static void main(String[] args) {
        SpringApplication.run(EtlExecutorApplication.class, args);
	}

}