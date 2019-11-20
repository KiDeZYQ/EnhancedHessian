package com.rampage.plugins.hessian;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.net.URL;

import com.caucho.hessian.client.HessianConnectionFactory;
import com.caucho.hessian.client.HessianProxyFactory;
import com.caucho.hessian.client.HessianURLConnectionFactoryWithProxy;
import com.caucho.hessian.io.HessianRemoteObject;

/**
 * the security hessian proxy factory
 * @author ziyuqi
 *
 */
public class EnhancedHessianProxyFactory  extends HessianProxyFactory {
	
	@Override
	public Object create(Class<?> api, URL url, ClassLoader loader) {
		if (api == null)
			throw new NullPointerException("api must not be null for HessianProxyFactory.create()");
		InvocationHandler handler = null;
		// user the EnhancedHessianProxy to process the request
		handler = new EnhancedHessianProxy(url, this, api);
		return Proxy.newProxyInstance(loader, new Class[]{api, HessianRemoteObject.class}, handler);
	}
	
	@Override
    protected HessianConnectionFactory createHessianConnectionFactory() {
        return new HessianURLConnectionFactoryWithProxy();
    }
}
