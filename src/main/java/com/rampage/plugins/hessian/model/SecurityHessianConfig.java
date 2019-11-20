package com.rampage.plugins.hessian.model;

import java.util.List;
import java.util.regex.Pattern;

import com.rampage.plugins.util.HttpServletRequestUtils;


/**
 * hessian安全配置封装，需要在xml文件中声明该bean
 * @author ziyuqi
 *
 */
public class SecurityHessianConfig {
	
	/**
	 * 密钥加密算法私钥
	 */
	private String privateKey;
	
	/**
	 * 密钥加密算法公钥
	 */
	private String publicKey;
	
	/**
	 * 如果密钥加密算法是对称算法，则可以直接指定key即可
	 */
	private String key;
	
	/**
	 * 是否开启认证： 开启之后启用签名，且默认为单向签名认证，即消息接收方对主动发起调用方的请求做验签
	 */
	private boolean openAuth;
	
	/**
	 * 是否开启双向认证：默认为false 只有在openAuth为true的时候，twoWayAuth为true才有意义。开启双向验签之后，框架内消息请求方也会对接收方响应做验签
	 */
	private boolean twoWayAuth;
	
	/**
	 * 密钥加密算法：默认AES。当前仅支持AES/DES/RSA三种算法
	 */
	private String algorithm;
	
	
	/**
	 * 签名算法：默认HmacSHA256, 不支持自定义
	 */
	private String signatureAlgorithm = "HmacSHA256";
	
	/**
	 * 在验签失败的时候是否抛出异常： 默认为true
	 */
	private boolean throwExceptionAuthFailed;
	
	/**
	 * 忽略校验的url正则表达式，为了防止没有将所有HessianProxyFactory全部改造成cacheable之前线上出现某个类的请求认证失败，通过在配置文件加入该配置指定过滤掉的认证请求即可
	 */
	private Pattern skipAuthPattern;
	
	/**
	 * 内网访问是否跳过安全验证
	 */
	private boolean skipIntranet = true; 
	
	/**
	 * IP白名单
	 */
	private List<String> ipWhiteList;
	
	public List<String> getIpWhiteList() {
		return ipWhiteList;
	}

	public void setIpWhiteList(List<String> ipWhiteList) {
		this.ipWhiteList = ipWhiteList;
	}

	public boolean isSkipIntranet() {
		return skipIntranet;
	}

	public void setSkipIntranet(boolean skipIntranet) {
		this.skipIntranet = skipIntranet;
	}

	public Pattern getSkipAuthPattern() {
		return skipAuthPattern;
	}
	
	public void setSkipAuthPattern(Pattern skipAuthPattern) {
		this.skipAuthPattern = skipAuthPattern;
	}



	public boolean isThrowExceptionAuthFailed() {
		return throwExceptionAuthFailed;
	}

	public void setThrowExceptionAuthFailed(boolean throwExceptionAuthFailed) {
		this.throwExceptionAuthFailed = throwExceptionAuthFailed;
	}

	public boolean isTwoWayAuth() {
		return twoWayAuth;
	}

	public void setTwoWayAuth(boolean twoWayAuth) {
		this.twoWayAuth = twoWayAuth;
	}

	public String getSignatureAlgorithm() {
		return signatureAlgorithm;
	}

	public boolean isOpenAuth() {
		return openAuth;
	}

	public String getPrivateKey() {
		return privateKey;
	}

	public void setPrivateKey(String privateKey) {
		this.privateKey = privateKey;
	}

	public String getPublicKey() {
		return publicKey;
	}

	public void setPublicKey(String publicKey) {
		this.publicKey = publicKey;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String getAlgorithm() {
		return algorithm;
	}

	public void setAlgorithm(String algorithm) {
		this.algorithm = algorithm;
	}

	public String getSignaturealgorithm() {
		return signatureAlgorithm;
	}

	public void setOpenAuth(boolean openAuth) {
		this.openAuth = openAuth;
	}
	
	/**
	 * 是否跳过安全认证：可以从请求uri来匹配或者ip方式来匹配
	 * @param uri  请求uri
	 * @param ip	请求ip
	 * @return	true，跳过安全校验；false，不跳过安全校验。
	 */
	public boolean skipAuth(String uri, String ip) {
		if (this.skipAuthPattern != null && this.skipAuthPattern.matcher(uri).find()) {
			return true;
		}
		
		if ((this.skipIntranet && HttpServletRequestUtils.isIntranetIp(ip)) || (this.ipWhiteList != null && this.ipWhiteList.contains(ip))) {
			return true;
		}
			
		return false;
	}
}
