package com.rampage.plugins.hessian.controller;

import javax.annotation.Resource;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.rampage.plugins.hessian.service.HelloService;

@RestController
@RequestMapping("/hessian/controller")
public class EnhancedController {
	
	@Resource(name = "helloClient")
	private HelloService helloService;
	
	@GetMapping("/showContext")
	public String showContext() {
		helloService.showContext();
		return "Watch the context in console!";
	}
}
