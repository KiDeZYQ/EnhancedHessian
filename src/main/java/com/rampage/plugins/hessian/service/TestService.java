package com.rampage.plugins.hessian.service;

import javax.annotation.Resource;

import org.springframework.stereotype.Service;

@Service
public class TestService {
	@Resource(name = "helloClient")
	private HelloService helloService;
}
