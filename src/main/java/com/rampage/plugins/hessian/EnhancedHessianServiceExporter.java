package com.rampage.plugins.hessian;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.Key;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.remoting.caucho.HessianServiceExporter;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.util.NestedServletException;

import com.caucho.hessian.client.HessianRuntimeException;
import com.rampage.plugins.hessian.model.SecurityHessianConfig;
import com.rampage.plugins.util.HessianAuthUtils;
import com.rampage.plugins.util.HttpServletRequestUtils;


/**
 * 支持安全机制的hessian发布服务
 * 
 * @author ziyuqi
 *
 */
public class EnhancedHessianServiceExporter extends HessianServiceExporter {

	private static final Logger LOG = LoggerFactory.getLogger(EnhancedHessianServiceExporter.class);
	
	/**
	 * Processes the incoming Hessian request and creates a Hessian response.
	 */
	@Override
	public void handleRequest(HttpServletRequest request, HttpServletResponse response)
	        throws ServletException, IOException {
		if (!"POST".equals(request.getMethod())) {
			throw new HttpRequestMethodNotSupportedException(request.getMethod(), new String[]{"POST"},
			        "HessianServiceExporter only supports POST requests");
		}
		response.setContentType(CONTENT_TYPE_HESSIAN);
		ByteArrayOutputStream baos = null;
		ByteArrayOutputStream resp = null;
		String ip = HttpServletRequestUtils.getRequestIp();
		LOG.debug("请求路径【{}】IP【{}】......", request.getRequestURI(), ip);
		try {
			SecurityHessianConfig securityHessianConfig = SecurityHessianConfigHolder.getSecurityConfig();
			if (securityHessianConfig.isOpenAuth()) {
				if (!securityHessianConfig.skipAuth(request.getRequestURI().toString(), ip)) {
					try {
						String signatureKey = request.getHeader(HessianAuthUtils.SIGNATURE_KEY);
						String signature = request.getHeader(HessianAuthUtils.SIGNATURE);
						if (StringUtils.isBlank(signatureKey) || StringUtils.isBlank(signature)) {
							LOG.error("收到请求路径【{}】IP【{}】, 接收端开启了hessian安全认证，但头信息未包含对应签名信息！", request.getRequestURI(), ip);
							throw new HessianRuntimeException("开启了hessian安全认证，但头信息未包含对应签名信息！");
						}
						Key key = HessianAuthUtils.decrypt(securityHessianConfig,
						        HessianAuthUtils.hexStringToByte(signatureKey));
						baos = new ByteArrayOutputStream();
						IOUtils.copy(request.getInputStream(), baos);
						// 比较接收到的信息签名和请求头签名是否匹配
						byte[] requestContentByte = baos.toByteArray();
						String localSignature = HessianAuthUtils.bytesToHexString(
						        HessianAuthUtils.getSignature(securityHessianConfig, key, baos.toByteArray()));
						if (!signature.equals(localSignature) && securityHessianConfig.isThrowExceptionAuthFailed()) {
							LOG.error("收到请求路径【{}】IP【{}】, hessian请求接收端验证签名不匹配！", request.getRequestURI(), ip);
							throw new HessianRuntimeException("hessian请求接收端验证签名不匹配！");
						}

						if (securityHessianConfig.isTwoWayAuth()) {
							resp = new ByteArrayOutputStream();
							invoke(new ByteArrayInputStream(requestContentByte) , resp);
							byte[] responseContentByte = resp.toByteArray();
							// 写入相应报文验签信息
							response.addHeader(HessianAuthUtils.SIGNATURE_KEY, HessianAuthUtils
							        .bytesToHexString(HessianAuthUtils.encrypt(securityHessianConfig, key)));
							response.addHeader(HessianAuthUtils.SIGNATURE, HessianAuthUtils.bytesToHexString(
							        HessianAuthUtils.getSignature(securityHessianConfig, key, responseContentByte)));
							response.getOutputStream().write(responseContentByte);
							response.getOutputStream().flush();
						} else {
							invoke(new ByteArrayInputStream(requestContentByte), response.getOutputStream());
						}
					} catch (Exception e) {
						LOG.error("收到请求路径【{}】IP【{}】, hessian请求接收端认证失败！", request.getRequestURI(), ip);
						throw new HessianRuntimeException("hessian请求接收端认证失败！", e);
					} finally {
						request.getInputStream().close();
						if (baos != null) {
							baos.close();
						}
						if (resp != null) {
							resp.close();
						}
					}
					return;
				}
			}
			invoke(request.getInputStream(), response.getOutputStream());
		} catch (Throwable ex) {
			throw new NestedServletException("Hessian skeleton invocation failed", ex);
		}
	}
}
