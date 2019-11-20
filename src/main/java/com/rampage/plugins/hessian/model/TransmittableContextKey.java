package com.rampage.plugins.hessian.model;

/**
 * 可传递上下文的key值
 * @author ziyuqi
 *
 */
public interface TransmittableContextKey {
	/**
	 * 链路标识
	 */
	String TRACE_ID = "traceId";
	
	/**
	 * 请求IP
	 */
	String IP = "IP";
}
