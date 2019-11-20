package com.rampage.plugins.hessian.publish;

import javax.annotation.Resource;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.remoting.caucho.HessianServiceExporter;

import com.rampage.plugins.hessian.EnhancedHessianServiceExporter;
import com.rampage.plugins.hessian.service.HelloService;

/**
 * hessian服务发布类
 * @author ziyuqi
 *
 */
@Configuration
public class HessianServerPublish {
	
	/**
	 * 注入发布的HelloService实现类
	 */
	@Resource(name="helloServer")
	private HelloService helloService;
	
	@Bean(name = "/hessian/helloService.hs")
	public HessianServiceExporter kettleExcutorService() {
		HessianServiceExporter exporter = new EnhancedHessianServiceExporter();
		exporter.setService(helloService);
		exporter.setServiceInterface(HelloService.class);
		return exporter;
	}
}
