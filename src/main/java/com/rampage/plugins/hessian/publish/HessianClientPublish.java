package com.rampage.plugins.hessian.publish;

import java.net.MalformedURLException;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.rampage.plugins.hessian.EnhancedHessianProxyFactory;
import com.rampage.plugins.hessian.service.HelloService;


/**
 * 创建hessian客户端服务实现，并且放入spring容器中
 * @author ziyuqi
 *
 */
@Configuration
public class HessianClientPublish {
	
	@Bean("helloClient")
	public HelloService helloClient(){
		EnhancedHessianProxyFactory proxyFactory = new EnhancedHessianProxyFactory();
		HelloService service = null;
		try {
			service = (HelloService) proxyFactory.create(HelloService.class, "http://localhost:8080/hessian/helloService.hs");
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		return service;
	}
}
