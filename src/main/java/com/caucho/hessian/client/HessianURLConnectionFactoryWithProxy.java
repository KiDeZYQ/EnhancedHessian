package com.caucho.hessian.client;

import java.io.IOException;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;


public class HessianURLConnectionFactoryWithProxy extends HessianURLConnectionFactory {
	private Proxy hostProxy;
	private HessianProxyFactory _proxyFactory;
	private String whiteListStr;
	private List<String> whiteList;
	
	public void setWhiteListStr(String _whiteListStr) {
		this.whiteListStr = _whiteListStr;
		if (whiteListStr != null && !"".equals(whiteListStr)) {
			whiteList = Arrays.asList(whiteListStr.split(","));
		} else {
			whiteList = null;
		}
	}
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
		boolean b = isHessianProxyWhiteList(url);
		if (hostProxy != null && b) {
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
		
		return connection;
	}

	/**
	 * 判断当前访问的服务器是否需要走代理
	 */
	private boolean isHessianProxyWhiteList(URL url) {
		// 获取请求服务器的ip
		if (url == null) {
			return false;
		}
		String host = url.getHost();
		if (whiteList != null && whiteList.contains(host)) {
			return true;
		}
		return false;
	}

}
