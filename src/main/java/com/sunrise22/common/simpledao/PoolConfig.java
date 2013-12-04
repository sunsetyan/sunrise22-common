/**
 * 
 */
package com.sunrise22.common.simpledao;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.sunrise22.common.tool.ExceptionTool;

/**
 * 就是一些静态属性,由于只有一个链接池实例，所以只做一个静态配置
 */
public class PoolConfig {
	
	private static String configFilePath = "/opt/fairy/ctrip/jdbc.ctrip.properties";
	
	private static final Log log = LogFactory.getLog(PoolConfig.class);
	
	private static Map<String, String> props = new HashMap<String, String>();
	
	public static String getProps(String key) {
		return props.get(key);
	}

	public static int getIntegerProps(String key) {
		return Integer.valueOf(props.get(key));
	}
	
	public static String getProps(String key, String def) {
		return props.get(key) == null ? props.get(key) : def;
	}
	
	static {
		load();
	}
	
	private static void load() {
		InputStreamReader in = null;
		try {
			in = new InputStreamReader(new FileInputStream(configFilePath), "utf-8");
			Properties dbField = new Properties();
			dbField.load(in);
			for (Object obj : dbField.keySet()) {
				props.put(obj.toString(), dbField.get(obj).toString());
			}
		} catch (UnsupportedEncodingException e) {
			ExceptionTool.printStackTrace(e);
		} catch (FileNotFoundException e) {
			ExceptionTool.printStackTrace(e);
			log.error(configFilePath + "配置文件不存在。");
		} catch (IOException e) {
			ExceptionTool.printStackTrace(e);
			log.error(configFilePath + "读取错误。");
		}
	}
	
	public static void reload() {
		load();
	}

}
