package com.rampage.plugins.util;

import java.io.IOException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import com.rampage.plugins.hessian.model.SecurityHessianConfig;

import sun.misc.BASE64Decoder;

/**
 * hessian鉴权工具类
 * @author ziyuqi
 *
 */
@SuppressWarnings("restriction")
public class HessianAuthUtils {
	public static final String ALGORITHM_AES = "AES";
	
	public static final String ALGORITHM_DES = "DES";
	
	public static final  String ALGORITHM_RSA = "RSA";
	
	public static final String DES_CRYPT_MODE = "DES/CBC/PKCS5Padding";
	
	public static final String IV_PARAMETER = "12345678";
	
	public static final String SIGNATURE_KEY = "SK";
	
	public static final String SIGNATURE = "S";
	
	private static final String HEX_STR = "0123456789ABCDEF";
	
	private HessianAuthUtils() {
	}
	
	public static byte[] encrypt(SecurityHessianConfig securityHessianConfig, Key sharedKey) throws Exception {
		if (ALGORITHM_AES.equalsIgnoreCase(securityHessianConfig.getAlgorithm())) {
			Cipher cipher = Cipher.getInstance(ALGORITHM_AES);
			cipher.init(Cipher.WRAP_MODE,
			        new SecretKeySpec((new BASE64Decoder()).decodeBuffer(securityHessianConfig.getPrivateKey()),
			                ALGORITHM_AES));
			return cipher.wrap(sharedKey);
		} else if (ALGORITHM_DES.equalsIgnoreCase(securityHessianConfig.getAlgorithm())) {
			DESKeySpec dks = new DESKeySpec((new BASE64Decoder()).decodeBuffer(securityHessianConfig.getPrivateKey()));
			SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance(ALGORITHM_DES);
			Cipher cipher = Cipher.getInstance(DES_CRYPT_MODE);
			IvParameterSpec iv = new IvParameterSpec(IV_PARAMETER.getBytes());
			cipher.init(Cipher.WRAP_MODE, secretKeyFactory.generateSecret(dks), iv);
			return cipher.wrap(sharedKey);
		} else if (ALGORITHM_RSA.equalsIgnoreCase(securityHessianConfig.getAlgorithm())) {
			KeyFactory keyFactory = KeyFactory.getInstance(ALGORITHM_RSA);
			PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(
			        (new BASE64Decoder()).decodeBuffer(securityHessianConfig.getPrivateKey()));
			Cipher cipher = Cipher.getInstance(ALGORITHM_RSA);
			cipher.init(Cipher.WRAP_MODE, keyFactory.generatePrivate(keySpec));
			return cipher.wrap(sharedKey);
		} else {
			throw new IOException("Not supported algorithm:" + securityHessianConfig.getAlgorithm() + "!");
			}
		}
	
	/**
	 * 对字节数组进行解密，得到用例签名的密钥
	 * @param securityHessianConfig  认证配置
	 * @param encKey   待解密的字节数组
	 * @return 签名的密钥
	 * @throws Exception
	 */
	public static Key decrypt(SecurityHessianConfig securityHessianConfig, byte[] encKey) throws Exception {
		if (ALGORITHM_AES.equalsIgnoreCase(securityHessianConfig.getAlgorithm())) {
			Cipher cipher = Cipher.getInstance(ALGORITHM_AES);
			cipher.init(Cipher.UNWRAP_MODE,
			        new SecretKeySpec((new BASE64Decoder()).decodeBuffer(securityHessianConfig.getPublicKey()),
			                ALGORITHM_AES));
			return cipher.unwrap(encKey, ALGORITHM_AES, Cipher.SECRET_KEY);
		} else if (ALGORITHM_DES
		        .equalsIgnoreCase(securityHessianConfig.getAlgorithm())) {
			Cipher cipher = Cipher.getInstance(DES_CRYPT_MODE);
			IvParameterSpec iv = new IvParameterSpec(IV_PARAMETER.getBytes());
			DESKeySpec dks = new DESKeySpec((new BASE64Decoder()).decodeBuffer(securityHessianConfig.getPublicKey()));
			SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance(ALGORITHM_DES);
			cipher.init(Cipher.UNWRAP_MODE, secretKeyFactory.generateSecret(dks), iv);
			return cipher.unwrap(encKey,ALGORITHM_DES, Cipher.SECRET_KEY);
		} else if (ALGORITHM_RSA
		        .equalsIgnoreCase(securityHessianConfig.getAlgorithm())) {
			Cipher keyCipher = Cipher.getInstance(ALGORITHM_RSA);
			KeyFactory keyFactory = KeyFactory.getInstance(ALGORITHM_RSA);
			X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(
			        (new BASE64Decoder()).decodeBuffer(securityHessianConfig.getPublicKey()));
			PublicKey pubKey = keyFactory.generatePublic(x509KeySpec);
			keyCipher.init(Cipher.UNWRAP_MODE, pubKey);
			return keyCipher.unwrap(encKey, ALGORITHM_RSA, Cipher.SECRET_KEY);
		} else {
			throw new IOException("Not supported algorithm:" + securityHessianConfig.getAlgorithm() + "!");
		}
	}
	
	/**
	 * 生成签名字节数组
	 * @param securityHessianConfig  安全配置
	 * @param key		签名密钥
	 * @param objectBytes	待签名的字节数组
	 * @return
	 * @throws Exception
	 */
	public static byte[] getSignature(SecurityHessianConfig securityHessianConfig, Key key, byte[] objectBytes) throws Exception {
		// 根据解密出来的密钥，用当前认证验签算法对对象字节流进行签名
		Mac mac = Mac.getInstance(securityHessianConfig.getSignaturealgorithm());	
		mac.init(key);
		return mac.doFinal(objectBytes);
	}
	
	/**
	 * 字节数组转16进制字符串
	 * @param bytes 字节数组
	 * @return 转换后的16进制字符串
	 */
	public static String bytesToHexString(byte[] bytes) {
		StringBuilder sb = new StringBuilder(bytes.length * 2);
		for(int i=0;i<bytes.length;i++){
			//字节高4位
			sb.append(String.valueOf(HEX_STR.charAt((bytes[i]&0xF0)>>4)));
			//字节低4位
			sb.append(String.valueOf(HEX_STR.charAt(bytes[i]&0x0F)));
		}
		
		return sb.toString();
	}
	
	/**
	 * 16进制字符串转字节数组
	 * @param hexString  16进制字符串
	 * @return 字节数组
	 */
	public static byte[] hexStringToByte(String hexString) {
		int len = hexString.length()/2;
		byte[] bytes = new byte[len];
		byte high = 0;   // 字节高四位
		byte low = 0;    // 字节低四位
		for(int i=0;i<len;i++){
			//右移四位得到高位
			high = (byte) ((HEX_STR.indexOf(hexString.charAt(2*i)))<<4);
			low = (byte) HEX_STR.indexOf(hexString.charAt(2*i+1));
			bytes[i] = (byte) (high|low); //高地位做或运算
		}
		return bytes;
	}
	
	
	public static void main(String[] args) throws Exception {
		byte[] bytes = new byte[]{-6, -114, -62, -75, 86, 115, 88, -16, 49, 95, -92, 105, 65, 116, 45, 15, 103, 72, -22, -86, -2, -63, -37, 48, -9, -12, -88, 97, -108, 7, -7, -72, 62, 99, 87, 110, 118, -75, 14, 15, 116, 120, -29, -99, -67, 87, 98, -29};
		System.out.println(Arrays.toString(bytes));
		String str = bytesToHexString(bytes);
		System.out.println(str);
		System.out.println(Arrays.toString(hexStringToByte(str)));
		/*KeyGenerator keyGen = KeyGenerator.getInstance("HmacSHA256");
		SecretKey sharedKey = keyGen.generateKey();
		
		// ----------------------------测试RSA加解密算法--------------------------------
		// 使用签名密钥加密算法私钥进行加密
		KeyFactory keyFactory = KeyFactory.getInstance(ALGORITHM_RSA);
		PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec((new BASE64Decoder()).decodeBuffer("MIICdgIBADANBgkqhkiG9w0BAQEFAASCAmAwggJcAgEAAoGBAMfJIBcATDADYDJdKD2kG/7ws8RTZtZ/Pgw0WoG7hlLsUTcjc7zLlVyBxTIdNOE6fk2sMBtf73YVSgCewxH2Rnd2IyQH8DamH/4kZY3v0FoTBWmVRP2opdyBE0V6BjK2Jsoda1eab8uB+X6Hrq79bDVCx6s44WxK0vFgRbsbt+rpAgMBAAECgYBCOW0lsreOgs/0YeTg6fFNxMQSJANGvs72hLQrpNmVyzfwqxPGVUWhcHLJddz9e8I6RQV9TYO3BL+PDRFkdtpMUNmnQkAj71dOloEpvyvJR5XGAgXDIP4H6mItQiO4InmsIfHcDJKgXR6KuBdd+3tSLMlJ8B6vwThQZ8WeBp5lcQJBAPIPN7uJZJHtUCZ0MZoL9NxMbg8iRPwRSCubZuVHCmiZnpAmBgCgbRKtRVgVgh/WCD8Z4UWODUryyqPogolpHKsCQQDTSqXIbPhEDP6fJ1RRXYmcXFcE15Jd2Scc7TJ7F79x32SjlxPevVoBw8OkkTtZoqXHETcmXhKXg44zOCZ32+67AkEA08uK6VWTILfzfGIIkJBLpIJffnBuydtZOYaU5qJdMh7QBbKvZ9b2+PORDDxtieuddZDwcgqUCPMdaYNkzFSjVwJAF0SEJyMg39WSgJJDLcagLdDZYFYg7ybHsN7KDVYbJf4UxhMQBBpT/BfDxq6bm45WtSpHKXl4kKjTEv7e9ZoJmwJAJG86+wDfzdqQx7M2bzVjhh4OYcohiJbdYkr/EOTRrzyS8JK/YuhCRvsZhxZPQVPBX26lKqwB3r7nfZDiBnbARQ=="));
		Cipher cipher = Cipher.getInstance(ALGORITHM_RSA);
		cipher.init(Cipher.WRAP_MODE, keyFactory.generatePrivate(keySpec));
		byte[] encryptedSignatureKey = cipher.wrap(sharedKey);
		System.out.println(Arrays.toString(encryptedSignatureKey));
		
		// 解密
		// 用公钥对加密后信息进行解密
        Cipher keyCipher = Cipher.getInstance(ALGORITHM_RSA);
		X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec((new BASE64Decoder()).decodeBuffer("MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDHySAXAEwwA2AyXSg9pBv+8LPEU2bWfz4MNFqBu4ZS7FE3I3O8y5VcgcUyHTThOn5NrDAbX+92FUoAnsMR9kZ3diMkB/A2ph/+JGWN79BaEwVplUT9qKXcgRNFegYytibKHWtXmm/Lgfl+h66u/Ww1QserOOFsStLxYEW7G7fq6QIDAQAB"));
        PublicKey pubKey = keyFactory.generatePublic(x509KeySpec);
        keyCipher.init(Cipher.UNWRAP_MODE, pubKey);
        Key key = keyCipher.unwrap(encryptedSignatureKey, ALGORITHM_RSA, Cipher.SECRET_KEY);
        System.out.println(Arrays.toString(key.getEncoded()));
        System.out.println(Arrays.toString(sharedKey.getEncoded()));
        
        // ----------------------------测试AES加解密算法---------------------------
        // 加密
        cipher = Cipher.getInstance(ALGORITHM_AES);
        cipher.init(Cipher.WRAP_MODE, new SecretKeySpec((new BASE64Decoder()).decodeBuffer("AAECAwQFBgcICWFiY2RlZg=="), ALGORITHM_AES));
        encryptedSignatureKey = cipher.wrap(sharedKey);
        System.out.println(Arrays.toString(encryptedSignatureKey));
        
        // 解密
        cipher = Cipher.getInstance(ALGORITHM_AES);
        cipher.init(Cipher.UNWRAP_MODE, new SecretKeySpec((new BASE64Decoder()).decodeBuffer("AAECAwQFBgcICWFiY2RlZg=="), ALGORITHM_AES));
        key = cipher.unwrap(encryptedSignatureKey, ALGORITHM_AES, Cipher.SECRET_KEY);
        System.out.println(Arrays.toString(key.getEncoded()));
        System.out.println(Arrays.toString(sharedKey.getEncoded()));
        
        
        // ----------------------------DES加解密-----------------------------------------
        // 加密
        DESKeySpec dks = new DESKeySpec((new BASE64Decoder()).decodeBuffer("AAECAwQFBgcICWFiY2RlZg=="));
        SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance(ALGORITHM_DES);
        cipher = Cipher.getInstance(DES_CRYPT_MODE);
        IvParameterSpec iv = new IvParameterSpec(IV_PARAMETER.getBytes());
        cipher.init(Cipher.WRAP_MODE, secretKeyFactory.generateSecret(dks), iv);
        encryptedSignatureKey = cipher.wrap(sharedKey);
        System.out.println(Arrays.toString(encryptedSignatureKey));
        
        // 解密
        cipher = Cipher.getInstance(DES_CRYPT_MODE);
        iv = new IvParameterSpec(IV_PARAMETER.getBytes());
        cipher.init(Cipher.UNWRAP_MODE, secretKeyFactory.generateSecret(dks), iv);
        key = cipher.unwrap(encryptedSignatureKey, ALGORITHM_DES, Cipher.SECRET_KEY);
        System.out.println(Arrays.toString(key.getEncoded()));
        System.out.println(Arrays.toString(sharedKey.getEncoded()));*/
	}
 }
