package com.rampage.plugins.hessian;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.security.Key;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

import org.apache.commons.lang3.StringUtils;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.caucho.hessian.client.HessianConnection;
import com.caucho.hessian.client.HessianProxy;
import com.caucho.hessian.client.HessianRuntimeException;
import com.caucho.hessian.client.SecurityHessianConnection;
import com.caucho.hessian.io.AbstractHessianOutput;
import com.rampage.plugins.hessian.model.SecurityHessianConfig;
import com.rampage.plugins.util.HessianAuthUtils;


/**
 * the security hessian proxy
 * @author ziyuqi
 *
 */
public class EnhancedHessianProxy extends HessianProxy  {
	private static final long serialVersionUID = 1L;

	private URL _url;
	
	private static final Logger LOG = LoggerFactory.getLogger(EnhancedHessianProxy.class);
	
	protected EnhancedHessianProxy(URL url, EnhancedHessianProxyFactory factory, Class<?> api, boolean enableHessianCache) {
		super(url, factory);
		this._url = url;
	}

	protected EnhancedHessianProxy(URL url, EnhancedHessianProxyFactory factory, Class<?> type) {
		super(url, factory, type);
		this._url = url;
	}
	
	/**
	 * Sends the HTTP request to the Hessian connection.
	 */
	protected HessianConnection sendRequest(String methodName, Object[] args) throws IOException {
		HessianConnection conn = null;

		conn = _factory.getConnectionFactory().open(_url);
		boolean isValid = false;
		ByteArrayOutputStream baos = null;
		AbstractHessianOutput out = null;
		try {
			addRequestHeaders(conn);
			baos = new ByteArrayOutputStream();
			out = _factory.getHessianOutput(baos);
			out.call(methodName, args);
			out.flush();
			// 如果开启了验证，这这里往头添加对应信息
			SecurityHessianConfig securityHessianConfig = SecurityHessianConfigHolder.getSecurityConfig();
			if (securityHessianConfig.isOpenAuth()) {
				try {
					javax.crypto.KeyGenerator keyGen = javax.crypto.KeyGenerator
					        .getInstance(securityHessianConfig.getSignaturealgorithm());
					Key sharedKey = keyGen.generateKey();
					// 使用签名密钥加密算法私钥进行加密
					byte[] encryptedSignatureKey = HessianAuthUtils.encrypt(securityHessianConfig, sharedKey);
					conn.addHeader(HessianAuthUtils.SIGNATURE_KEY,
					        HessianAuthUtils.bytesToHexString(encryptedSignatureKey));
					conn.addHeader(HessianAuthUtils.SIGNATURE, HessianAuthUtils.bytesToHexString(
					        HessianAuthUtils.getSignature(securityHessianConfig, sharedKey, baos.toByteArray())));
				} catch (Exception e) {
					throw new HessianRuntimeException("hessian请求发送端认证【加签】失败！", e);
				}
			}

			OutputStream os = null;
			try {
				os = conn.getOutputStream();
			} catch (Exception e) {
				throw new HessianRuntimeException(e);
			}
			os.write(baos.toByteArray());
			os.flush();

			conn.sendRequest();

			isValid = true;

			return conn;
		} finally {
			if (!isValid && conn != null)
				conn.destroy();
			if (out != null) {
				out.close();
			}
		}
	}

	@Override
	protected InputStream getInputStream(HessianConnection conn) throws IOException {
		SecurityHessianConfig securityHessianConfig = SecurityHessianConfigHolder.getSecurityConfig();
		if (securityHessianConfig.isOpenAuth() && securityHessianConfig.isTwoWayAuth()) {
			SecurityHessianConnection securityConnection = (SecurityHessianConnection) conn;
			if (!securityHessianConfig.skipAuth(_url.toString(), _url.getHost())) {
				String signatureKey = securityConnection.getSignatureKey();
				String signature = securityConnection.getSignature();
				if (StringUtils.isBlank(signatureKey) || StringUtils.isBlank(signature)) {
					LOG.error("请求URL【{}】IP【{}】，开启了hessian安全认证，但响应头信息未包含对应签名信息！", _url, _url.getHost());
					throw new HessianRuntimeException("开启了hessian安全认证，但响应头信息未包含对应签名信息！");
				}
				ByteArrayOutputStream baos = null;
				try {
					Key key = HessianAuthUtils.decrypt(securityHessianConfig,
					        HessianAuthUtils.hexStringToByte(signatureKey));
					baos = new ByteArrayOutputStream();
					IOUtils.copy(conn.getInputStream(), baos);
					// 得到本地签名
					byte[] responseContentByte = baos.toByteArray();
					String localSignature = HessianAuthUtils.bytesToHexString(
					        HessianAuthUtils.getSignature(securityHessianConfig, key, baos.toByteArray()));
					if (!signature.equals(localSignature) && securityHessianConfig.isThrowExceptionAuthFailed()) {
						LOG.error("请求URL【{}】IP【{}】，hessian请求发送端验证返回信息签名不匹配！", _url, _url.getHost());
						throw new HessianRuntimeException("hessian请求发送端验证返回信息签名不匹配！");
					}
					ByteArrayInputStream bis = new ByteArrayInputStream(responseContentByte);
					if ("deflate".equals(conn.getContentEncoding())) {
						return new InflaterInputStream(bis, new Inflater(true));
					}
					return bis;
				} catch (Exception e) {
					LOG.error("请求URL【{}】IP【{}】，hessian请求发送端认证验签失败！", _url, _url.getHost());
					throw new HessianRuntimeException("hessian请求发送端认证验签失败！", e);
				} finally {
					if (baos != null) {
						baos.close();
					}
					conn.getInputStream().close(); // 原来connectionn的输入流可以关闭了
				}
			}
		}

		// 没有开启双向认证
		if ("deflate".equals(conn.getContentEncoding())) {
			return new InflaterInputStream((InputStream) conn.getInputStream(), new Inflater(true));
		}
		return conn.getInputStream();
	}
}
