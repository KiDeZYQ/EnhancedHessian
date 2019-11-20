package com.rampage.plugins.util;

import java.util.Map;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * HttpServletRequest的工具类
 */
public class HttpServletRequestUtils {
	
	private static final Log logger = LogFactory.getLog(HttpServletRequestUtils.class);

	public static HttpServletRequest getRequest() {
		RequestAttributes reqAttr = RequestContextHolder.getRequestAttributes();
		if (reqAttr != null && reqAttr instanceof ServletRequestAttributes) {
			return ((ServletRequestAttributes) reqAttr).getRequest();
		}

		return null;
	}

	public static HttpServletResponse getResponse() {
		RequestAttributes reqAttr = RequestContextHolder.getRequestAttributes();
		if (reqAttr != null && reqAttr instanceof ServletRequestAttributes) {
			return ((ServletRequestAttributes) reqAttr).getResponse();
		}

		return null;
	}

	public static boolean isRpcCall(HttpServletRequest request) {
		if (request == null)
			return false;

		/**
		 * 如果是hessian请求则认为是rpc调用
		 */
		if (isHessianRequest(request))
			return true;

		/**
		 * 如果是soap的web service请求则认为是rpc调用
		 */
		if (isSoapRequest(request))
			return true;

		return false;
	}

	/**
	 * 判断是否为soap调用
	 * 
	 * @param request
	 * @return
	 */
	private static boolean isSoapRequest(HttpServletRequest request) {
		if (request == null)
			return false;

		String contentType = request.getContentType();
		if (contentType == null)
			return false;

		/*
		 * sopa1.2协议中，ContentType为application/soap+xml
		 */
		if (contentType.contains("soap"))
			return true;

		/**
		 * sopa协议中有一个请求头SOAPAction的项，该项可能为空串 因此只判断是否为null
		 */
		if (request.getHeader("SOAPAction") != null)
			return true;

		return false;
	}

	/**
	 * 判断是否为hessian调用
	 * 
	 * @param request
	 * @return
	 */
	private static boolean isHessianRequest(HttpServletRequest request) {
		if (request == null)
			return false;

		String contentType = request.getContentType();
		if (contentType == null)
			return false;

		/*
		 * Hessian协议的ContentType为x-application/hessian
		 */
		if (contentType.contains("hessian"))
			return true;

		return false;
	}

	private static String getFirstValidIp(String ipList) {
		if (ipList == null || ipList.length() == 0)
			return null;

		String[] ips = ipList.split(",");
		for (String ip : ips) {
			if (StringUtils.isBlank(ip))
				continue;

			if ("unknown".equalsIgnoreCase(ip))
				continue;

			return ip;
		}

		return null;
	}
	
	/**
	 * 空白的IP也当做内网IP  
	 * @param ip ip字符串
	 * @return  true，是内网IP;false, 不是内网IP
	 */
	public static boolean isIntranetIp(String ip) {
		if (StringUtils.isBlank(ip)) {
			return false;
		}
		
		if (ip.equalsIgnoreCase("localhost")) {
			return true;
		}

		long ipNum = getIpNum(ip);
		/**
		 * 私有IP：A类 10.0.0.0-10.255.255.255 
		 * 			   B类 172.16.0.0-172.31.255.255 
		 * 			   C类 192.168.0.0-192.168.255.255 当然，还有127这个网段是环回地址
		 **/
		long aBegin = getIpNum("10.0.0.0");
		long aEnd = getIpNum("10.255.255.255");
		long bBegin = getIpNum("172.16.0.0");
		long bEnd = getIpNum("172.31.255.255");
		long cBegin = getIpNum("192.168.0.0");
		long cEnd = getIpNum("192.168.255.255");
		boolean isInnerIp = isInner(ipNum, aBegin, aEnd) || isInner(ipNum, bBegin, bEnd) || isInner(ipNum, cBegin, cEnd)
		        || ip.equals("127.0.0.1");
		return isInnerIp;    
	}
	
	private static long getIpNum(String ipAddress) {   
	    String [] ip = ipAddress.split("\\.");   
	    long a = Integer.parseInt(ip[0]);   
	    long b = Integer.parseInt(ip[1]);   
	    long c = Integer.parseInt(ip[2]);   
	    long d = Integer.parseInt(ip[3]);   
	  
	    long ipNum = a * 256 * 256 * 256 + b * 256 * 256 + c * 256 + d;   
	    return ipNum;   
	} 
	
	private static boolean isInner(long userIp,long begin,long end){   
	     return (userIp>=begin) && (userIp<=end);   
	}  

	public static String getRequestIp() {
		HttpServletRequest request = HttpServletRequestUtils.getRequest();
		if (request == null)
			return null;

		String ip = getFirstValidIp(request.getHeader("X-Forwarded-For"));

		if (ip == null) {
			ip = getFirstValidIp(request.getHeader("X-FORWARDED-FOR"));
		} else {
			return ip;
		}

		if (ip == null) {
			ip = getFirstValidIp(request.getHeader("Proxy-Client-IP"));
		} else {
			return ip;
		}

		if (ip == null) {
			ip = getFirstValidIp(request.getHeader("WL-Proxy-Client-IP"));
		} else {
			return ip;
		}

		if (ip == null) {
			ip = getFirstValidIp(request.getHeader("HTTP_CLIENT_IP"));
		} else {
			return ip;
		}

		if (ip == null) {
			ip = getFirstValidIp(request.getHeader("HTTP_X_FORWARDED_FOR"));
		} else {
			return ip;
		}
		
		if (ip == null) {
			ip = getFirstValidIp(request.getHeader("X-Real-IP"));
		} else {
			return ip;
		}

		if (ip == null) {
			ip = request.getRemoteAddr();
		} else {
			return ip;
		}

		if (ip != null)
			return ip.trim();
		else
			return null;
	}

	/**
	 * 获取原始的http请求的内容，主要用于获取web接口中请求内容
	 * 
	 * @param request
	 * @return
	 */
	public static String getRequestString(HttpServletRequest request) {
		if (request == null)
			return null;

		/**
		 * 如果是rpc调用，则不获取请求内容，rpc调用请求的内容是特定格式
		 */
		if (HttpServletRequestUtils.isRpcCall(request))
			return null;

		/**
		 * 是GET方法则从query string中获取
		 */
		String method = request.getMethod();
		if (method==null)
			method = StringUtils.EMPTY;
		
		if (method.equalsIgnoreCase("GET"))
			return request.getQueryString();

		/**
		 * 如果是post方法则从请求的body中获取,但需要区分文件上传的 情况
		 */
		if (method.equalsIgnoreCase("POST")) {
			try {
				ServletInputStream inputStream = request.getInputStream();
				int length = request.getContentLength();
				if (length <= 0)
					return null;

				byte[] bytes = new byte[length];
				int readSize = inputStream.read(bytes);
				if (readSize > 0)
					return new String(bytes, 0, readSize);
				else
					return StringUtils.EMPTY;
			} catch (Throwable t) {
				logger.error("get post data body from request input stream fail", t);
			}
		}

		return null;
	}

	public static boolean isMultipart(HttpServletRequest request) {
		if (!isHttpPost(request)) {
			return false;
		}

		String contentType = request.getContentType();
		return (contentType != null && contentType.toLowerCase().startsWith("multipart/"));
	}

	public static boolean isHttpPost(HttpServletRequest request) {
		if (request == null)
			return false;
		
		String method = request.getMethod();
		if (method == null || !"post".equalsIgnoreCase(method)) {
			return false;
		}

		return true;
	}

	/**
	 * 检查http请求是否是请求的传入的二进制数据，对于octet-stream，image，multipart文件 都认为是二进制的
	 * 
	 * @param request
	 * @return
	 */
	public static boolean isBinayBodyData(HttpServletRequest request) {
		String contentType = request.getContentType();
		if (contentType == null)
			return false;
		contentType = contentType.toLowerCase();

		// 判断Content-Type是否指定为流数据
		if (contentType.contains("stream"))
			return true;

		// 判断Content-Type是否指定为文件上传
		if (contentType.contains("multipart"))
			return true;

		// 判断Content-Type是否指定为图片
		if (contentType.contains("image"))
			return true;

		return false;
	}

	public static String getParameterMapString(HttpServletRequest request) {
		if (request == null)
			return StringUtils.EMPTY;

		Map<String, String[]> map = request.getParameterMap();

		if (map == null || map.size() <= 0)
			return StringUtils.EMPTY;

		StringBuilder sb = new StringBuilder(100);
		boolean bfirst = true;// 是否首次拼接
		for (Map.Entry<String, String[]> entry : map.entrySet()) {
			for (String item : entry.getValue()) {
				if (!bfirst) {
					sb.append("&");
				} else {
					bfirst = false;
				}
				sb.append(entry.getKey());
				sb.append("=");
				sb.append(item);
			}
		}

		return sb.toString();
	}
}
