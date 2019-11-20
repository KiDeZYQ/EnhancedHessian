package com.rampage.plugins.hessian.service;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.alibaba.ttl.threadpool.TtlExecutors;
import com.rampage.plugins.hessian.model.TransmittableContextKey;
import com.rampage.plugins.util.TransmittableContext;

/**
 * 
 * @author ziyuqi
 *
 */
@Service
public class EnhancedShowContextService {
	/**
	 * 得到一个被包装的线程池，该线程池中的线程能获取当前上下文
	 */
	private ExecutorService executorService = TtlExecutors.getTtlExecutorService(Executors.newFixedThreadPool(3));
	
	private static final Logger LOG = LoggerFactory.getLogger(EnhancedShowContextService.class);

	public void showContext() {
		new Thread() {
			@Override
			public void run() {
				LOG.info("Trace id in subThread is {}", TransmittableContext.get(TransmittableContextKey.TRACE_ID));
			}
		}.start();
		
		for (int i = 0; i < 10; i++) {
			executorService.submit(new Runnable() {
				@Override
				public void run() {
					LOG.info("Trace id in threadPool subThread {} is {}", Thread.currentThread().getName(),
					        TransmittableContext.get(TransmittableContextKey.TRACE_ID));
				}
			});
		}
	}

	@PreDestroy
	public void preDestroy() {
		executorService.shutdown();
		try {
			executorService.awaitTermination(10, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			LOG.info("Await shutDown failed!", e);
			executorService.shutdownNow();
		}
		LOG.info("线程池销毁成功!");
	}
}
