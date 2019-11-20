package com.caucho.hessian.client;

import java.io.IOException;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.rampage.plugins.util.TransmittableContext;


public class HessianURLConnectionFactoryWithProxy extends HessianURLConnectionFactory {
	private Proxy hostProxy;
	private HessianProxyFactory _proxyFactory;
	
	public void setHostProxy(Proxy _proxy){
		this.hostProxy=_proxy;
	}

	private static final Logger log = Logger
			.getLogger(HessianURLConnectionFactoryWithProxy.class.getName());

	
	public void setHessianProxyFactory(HessianProxyFactory factory) {
		super.setHessianProxyFactory(factory);
		_proxyFactory = factory;
	}

	public HessianConnection open(URL url) throws IOException {
		if (log.isLoggable(Level.FINER)) {
			log.finer(this + " open(" + url + ")");
		}
		URLConnection conn;
		if (hostProxy != null) {
			conn = url.openConnection(hostProxy);
		} else {
			conn = url.openConnection();
		}
		long connectTimeout = this._proxyFactory.getConnectTimeout();

		if (connectTimeout >= 0L) {
			conn.setConnectTimeout((int) connectTimeout);
		}
		conn.setDoOutput(true);

		long readTimeout = this._proxyFactory.getReadTimeout();

		if (readTimeout > 0L) {
			try {
				conn.setReadTimeout((int) readTimeout);
			} catch (Throwable e) {
			}
		}

		// 往头添加日志跟踪相关信息
		HessianConnection connection =  new SecurityHessianConnection(url, conn);
		Map<String, String> context = TransmittableContext.getCurrentContext();
		if (context != null) {
			for (Entry<String, String> entry : context.entrySet()) {
				connection.addHeader(entry.getKey(), entry.getValue());
			}
		}
		return connection;
	}
}
