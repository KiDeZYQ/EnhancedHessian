package com.rampage.plugins.util;

import java.util.HashMap;
import java.util.Map;

import com.alibaba.ttl.TransmittableThreadLocal;

public class TransmittableContext {
	/**
	 * 上下文
	 */
	private static TransmittableThreadLocal<Map<String, String>> context = new TransmittableThreadLocal<Map<String, String>>();

	/**
	 * 得到当前上下文
	 * @return  当前上下文
	 */
	public static Map<String, String> getCurrentContext() {
		return context.get() == null ? null : new HashMap<String, String>(context.get());
	}

	public static void push(String key, String value) {
		Map<String, String> contextMap = context.get();
		Map<String, String> curMap = null;
		// 每个Map都线程独立，父线程清除Map不会影响子线程
		if (contextMap != null) {
			curMap = new HashMap<String, String>(contextMap);
		} else {
			curMap = new HashMap<String, String>();
		}
		curMap.put(key, value);
		context.set(curMap);
	}
	
	public static String get(String key) {
		return context.get() == null ? null : context.get().get(key);
	}
	
	public static void remove(String key) {
		Map<String, String> curMap = context.get();
		if (curMap != null) {
			//  更新当前线程Map，不影响子线程中的上下文
			final Map<String, String> copy = new HashMap<String, String>(curMap);
            copy.remove(key);
            context.set(copy);
		}
	}
	
	public static void clear() {
		context.remove();
	}
}
