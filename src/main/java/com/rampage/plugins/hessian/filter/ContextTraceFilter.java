package com.rampage.plugins.hessian.filter;

import java.io.IOException;
import java.util.UUID;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.springframework.core.annotation.Order;

import com.rampage.plugins.hessian.model.TransmittableContextKey;
import com.rampage.plugins.util.HttpServletRequestUtils;
import com.rampage.plugins.util.TransmittableContext;

/**
 * 上下文拦截器，将请求头中的上线文信息放入当前应用的上下文存储器中 实现上下文在调用链路中传递
 * @author ziyuqi
 *
 */
@WebFilter(urlPatterns = {"/*"}, filterName = "contextFilter")
@Order(1)	// 设置顺序，越小优先级越高，这里将该过滤器设置为最先执行
public class ContextTraceFilter implements Filter {

	@Override
	public void destroy() {
		System.out.println("Destroy the contextFilter!");
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain)
	        throws IOException, ServletException {
		// 设置当前traceId
		HttpServletRequest httpReq = (HttpServletRequest) request;
		String traceId = httpReq.getHeader(TransmittableContextKey.TRACE_ID);
		if (StringUtils.isEmpty(traceId)) {
			TransmittableContext.push(TransmittableContextKey.TRACE_ID, UUID.randomUUID().toString().replace("-", ""));
		} else {
			TransmittableContext.push(TransmittableContextKey.TRACE_ID, traceId);
			// 如果有传入操作标识，则此时需要将操作标识放入全局变量中
			String operationType = httpReq.getHeader(traceId);
			if (StringUtils.isNotBlank(operationType)) {
				TransmittableContext.push(traceId, operationType);
			}
		}

		// 设置请求IP
		String ip = httpReq.getHeader(TransmittableContextKey.IP);
		if (ip != null) {
			TransmittableContext.push(TransmittableContextKey.IP, ip);
		} else {
			TransmittableContext.push(TransmittableContextKey.IP, HttpServletRequestUtils.getRequestIp());
		}
		
		try {
			filterChain.doFilter(request, response);
		} finally {
			TransmittableContext.clear();
		}
	}

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		System.out.println("Init the contextFilter!");
	}

}
