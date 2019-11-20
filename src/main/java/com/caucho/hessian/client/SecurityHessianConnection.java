package com.caucho.hessian.client;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

import com.rampage.plugins.util.HessianAuthUtils;


/**
 * 支持安全的hessian连接
 * @author ziyuqi
 *
 */
public class SecurityHessianConnection extends HessianURLConnection  {

	private String signatureKey;
	
	private String signature;
	
	private URL url;
	
	public SecurityHessianConnection(URL url, URLConnection conn) {
		super(url, conn);
		this.url = url;
	}

	@Override
    protected void parseResponseHeaders(HttpURLConnection conn)
            throws IOException {
		this.signatureKey = conn.getHeaderField(HessianAuthUtils.SIGNATURE_KEY);
		this.signature = conn.getHeaderField(HessianAuthUtils.SIGNATURE);
    }

	public String getSignatureKey() {
		return signatureKey;
	}

	public String getSignature() {
		return signature;
	}

	public URL getUrl() {
		return url;
	}
}
