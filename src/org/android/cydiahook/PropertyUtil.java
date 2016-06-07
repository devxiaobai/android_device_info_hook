package org.android.cydiahook;

import java.lang.reflect.Method;

import android.util.Log;

/**
 * 系统属性配置和读取帮助类
 * 
 * @author baiyingjun
 * 
 */
public class PropertyUtil {

	public static void set(String key, String val) {
		try {
			@SuppressWarnings("rawtypes")
			Class SystemProperties = Class
					.forName("android.os.SystemProperties");

			Method set1 = SystemProperties.getMethod("set", new Class[] {
					String.class, String.class });
			Object resutl = set1.invoke(SystemProperties, new Object[] { key,
					val });
		} catch (Exception e) {
			e.printStackTrace();
			Log.d("cydCrash", e.getMessage());
		}
	}

	public static String read(String key) {
		@SuppressWarnings("rawtypes")
		Class SystemProperties;
		try {
			SystemProperties = Class.forName("android.os.SystemProperties");
			// Parameters Types
			@SuppressWarnings("rawtypes")
			Class[] paramTypes = new Class[1];
			paramTypes[0] = String.class;

			Method get = SystemProperties.getMethod("get", paramTypes);

			// Parameters
			Object[] params = new Object[1];
			params[0] = new String(key);

			return (String) get.invoke(SystemProperties, params);
		} catch (Exception e) {
			e.printStackTrace();	
			Log.d("cydCrash", e.getMessage());
		}

		return "null";
	}
}
