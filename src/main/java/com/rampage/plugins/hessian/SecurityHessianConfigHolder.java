package com.rampage.plugins.hessian;

import java.util.Arrays;
import java.util.Locale;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;

import com.rampage.plugins.hessian.model.SecurityHessianConfig;

/**
 * hessian安全配置的存储类
 * @author ziyuqi
 *
 */
public class SecurityHessianConfigHolder {
	
	private static ReloadableResourceBundleMessageSource source = new ReloadableResourceBundleMessageSource();
	
	static {
		source.setBasename("application");
		source.setDefaultEncoding("utf-8");
		source.setCacheSeconds(30);
	}
	
	private static String getValue(String key, String defaultValue) {
		return source.getMessage(key, null, defaultValue, Locale.getDefault());
	}
	
	public static SecurityHessianConfig getSecurityConfig() {
		SecurityHessianConfig config = new SecurityHessianConfig();
		config.setAlgorithm(getValue("hessian.auth.algorithm", "AES"));
		config.setOpenAuth(Boolean.valueOf(getValue("hessian.auth.open", "false")));
		config.setTwoWayAuth(Boolean.valueOf(getValue("hessian.auth.twoWayAuth", "false")));
		config.setKey(getValue("hessian.auth.key", "AAECAwQFBgcICWFiY2RlZg=="));
		config.setPrivateKey(getValue("hessian.auth.privateKey", null));
		config.setPublicKey(getValue("hessian.auth.publicKey", null));
		config.setThrowExceptionAuthFailed(Boolean.valueOf(getValue("hessian.auth.throwExceptionAuthFailed", "true")));
		config.setSkipIntranet(Boolean.valueOf(getValue("hessian.auth.skipIntranet", "true")));
		config.setIpWhiteList(Arrays.asList(getValue("hessian.auth.ipWhiteList", StringUtils.EMPTY).split(";")));
		String skipReg = getValue("hessian.auth.skipReg", null);
		if (skipReg != null && !skipReg.isEmpty()) {
			config.setSkipAuthPattern(Pattern.compile(skipReg));
		}
		if (config.getPrivateKey() == null && config.getPublicKey() == null) {
			config.setPrivateKey(config.getKey());
			config.setPublicKey(config.getKey());
		}
		
		return config;
	}
}
