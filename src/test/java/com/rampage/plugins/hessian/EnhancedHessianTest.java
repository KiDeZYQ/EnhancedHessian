package com.rampage.plugins.hessian;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import javax.annotation.Resource;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.caucho.hessian.client.HessianConnectionException;
import com.caucho.hessian.client.HessianProxyFactory;
import com.rampage.plugins.Application;
import com.rampage.plugins.hessian.controller.EnhancedController;
import com.rampage.plugins.hessian.service.HelloService;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {Application.class}) // 指定启动类
@WebAppConfiguration // 开启web应用配置
public class EnhancedHessianTest {
	@Resource(name = "helloClient")
	private HelloService helloService;

	@Autowired
	private EnhancedController enhancedHessianController;
	
	private MockMvc mvc;

	@Before
	public void setUp() throws Exception {
		mvc = MockMvcBuilders.standaloneSetup(enhancedHessianController).build();
	}

	// 测试安全验证通过，此时的hessian客户端是通过增强的HessianProxyFactory创建的
	@Test
	public void testSecurityInvokePass() {
		// 直接通过增强的HessianProxyFactory认证初始化后，能够通过安全认证调用成功
		helloService.sayHello("KiDe");
	}

	// 注意此处不是抛出接收端验签不通过的 HessianRuntimeException 而是
	// 接收端出现错误导致调用的出现的HessianConnectionException
	@Test(expected = HessianConnectionException.class)
	public void testSecurityInvokeFailed() throws Exception {
		// 不使用增强的HessianProxyFactory初始化客户端调用无法通过安全认证
		HessianProxyFactory proxyFactory = new HessianProxyFactory();
		HelloService service = (HelloService) proxyFactory.create(HelloService.class,
		        "http://localhost:8080/hessian/helloService.hs");
		service.sayHello("KiDe");
	}
	
	 @Test
    public void hello() throws Exception {
		 // 两次请求的traceId打印应该不同
        mvc.perform(
            MockMvcRequestBuilders
            .get("/hessian/controller/showContext")
            .accept(MediaType.APPLICATION_JSON_UTF8)
        )
        .andExpect(status().isOk()) // 用于判断返回的期望值
        .andExpect(content().string(equalTo("Watch the context in console!")));
        
        mvc.perform(
                MockMvcRequestBuilders
                .get("/hessian/controller/showContext")
                .accept(MediaType.APPLICATION_JSON_UTF8)
            )
            .andExpect(status().isOk()) // 用于判断返回的期望值
            .andExpect(content().string(equalTo("Watch the context in console!")));
    }  

}
