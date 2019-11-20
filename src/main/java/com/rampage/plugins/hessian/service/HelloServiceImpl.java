package com.rampage.plugins.hessian.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.rampage.plugins.hessian.model.TransmittableContextKey;
import com.rampage.plugins.util.TransmittableContext;

/**
 * HelloService的实现类
 * @author ziyuqi
 *
 */
@Service("helloServer")
public class HelloServiceImpl implements HelloService {
	@Autowired
	private EnhancedShowContextService showContextService;
	
	@Override
	public void sayHello(String name) {
		System.out.println("Hello " + name);
	}

	@Override
	public void showContext() {
		System.out.println("Trace id in HelloService is: " + TransmittableContext.get(TransmittableContextKey.TRACE_ID));
		showContextService.showContext();
	}

}
