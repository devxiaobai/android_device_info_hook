package org.android.cydiahook;

import java.lang.reflect.Method;

import android.content.ContentResolver;

import com.saurik.substrate.MS;
import com.saurik.substrate.MS.ClassLoadHook;

public class Main {

	static final String key = "sys.rw.Deviceindex";

	static int readIndex() {
		String index = PropertyUtil.read(key);
		int in;
		try {
			in = Integer.parseInt(index);
		} catch (NumberFormatException e) {
			return 0;
		}
		return in;
	}

	static void initialize() {
		// getExternalStorageState hook to return true, will cause boot issue
		// so hook it after boot
		// if (!MainActivity.isManualTrigger) return;
		//
		// MS.hookClassLoad("android.content.res.Resources",
		// new MS.ClassLoadHook() {
		// public void classLoaded(Class<?> resources) {
		//
		// Method getColor;
		// try {
		// getColor = resources.getMethod("getColor",
		// Integer.TYPE);
		// } catch (NoSuchMethodException e) {
		// getColor = null;
		// }
		//
		// if (getColor != null) {
		// MS.hookMethod(
		// resources,
		// getColor,
		// new MS.MethodAlteration<Resources, Integer>() {
		// public Integer invoked(
		// Resources resources,
		// Object... args)
		// throws Throwable {
		// return invoke(resources, args)
		// & ~0x0000ff00 | 0x00ff0000;
		// }
		// });
		// }
		// }
		// });
		
		MS.hookClassLoad("android.os.SystemProperties", new ClassLoadHook() {
			
			@Override
			public void classLoaded(Class<?> clz) {
				// hook getString(String pro)
				Method methodGetString;
				try {
					methodGetString = clz.getMethod("get",
							 String.class,String.class);
				} catch (NoSuchMethodException e) {
					methodGetString = null;
				}
				if (methodGetString != null) {

					final MS.MethodPointer old = new MS.MethodPointer();

					MS.hookMethod(clz, methodGetString,
							new MS.MethodHook() {
								@Override
								public Object invoked(Object obj,
										Object... args)
										throws Throwable {
									if ("ro.product.brand".equals(String
											.valueOf(args[0]))) {
										int index = readIndex();
										return brands[index];
									}if ("ro.product.model".equals(String
											.valueOf(args[0]))) {
										int index = readIndex();
										return models[index];
									}
									return old.invoke(obj, args);
								}
							}, old);

				}
			}
		});

		MS.hookClassLoad("android.provider.Settings$Secure",
				new ClassLoadHook() {

					@Override
					public void classLoaded(Class<?> clz) {
						// hook getAndroidId
						Method methodGetAndroidId;
						try {
							methodGetAndroidId = clz.getMethod("getString",
									ContentResolver.class, String.class);
						} catch (NoSuchMethodException e) {
							methodGetAndroidId = null;
						}
						if (methodGetAndroidId != null) {

							final MS.MethodPointer old = new MS.MethodPointer();

							MS.hookMethod(clz, methodGetAndroidId,
									new MS.MethodHook() {
										@Override
										public Object invoked(Object obj,
												Object... args)
												throws Throwable {
											if ("android_id".equals(String
													.valueOf(args[1]))) {
												int index = readIndex();
												return andIds[index];
											}
											return old.invoke(obj, args);
										}
									}, old);

						}
					}
				});

		MS.hookClassLoad("android.net.wifi.WifiInfo", new ClassLoadHook() {

			@Override
			public void classLoaded(Class<?> clz) {
				// hook getMacAddress
				Method methodGetMac;
				try {
					methodGetMac = clz.getMethod("getMacAddress",
							new Class<?>[0]);
				} catch (NoSuchMethodException e) {
					methodGetMac = null;
				}
				if (methodGetMac != null) {
					final MS.MethodPointer old = new MS.MethodPointer();

					MS.hookMethod(clz, methodGetMac, new MS.MethodHook() {
						@Override
						public Object invoked(Object obj, Object... args)
								throws Throwable {
							int index = readIndex();
							return macs[index];
						}
					}, old);
				}
			}
		});

		MS.hookClassLoad("android.telephony.TelephonyManager",
				new MS.ClassLoadHook() {

					@Override
					public void classLoaded(Class<?> clz) {

						// hook getDeviceId
						Method methodGetImei;
						try {
							methodGetImei = clz.getMethod("getDeviceId",
									new Class<?>[0]);
						} catch (NoSuchMethodException e) {
							methodGetImei = null;
						}
						if (methodGetImei != null) {
							final MS.MethodPointer old = new MS.MethodPointer();

							MS.hookMethod(clz, methodGetImei,
									new MS.MethodHook() {
										@Override
										public Object invoked(Object obj,
												Object... args)
												throws Throwable {
											int index = readIndex();
											return deviceIds[index];
										}
									}, old);
						}

						// hook getSubscriberId
						Method methodGetIMSI;
						try {
							methodGetIMSI = clz.getMethod("getSubscriberId",
									new Class<?>[0]);
						} catch (NoSuchMethodException e) {
							methodGetIMSI = null;
						}
						if (methodGetIMSI != null) {
							final MS.MethodPointer old = new MS.MethodPointer();

							MS.hookMethod(clz, methodGetIMSI,
									new MS.MethodHook() {
										@Override
										public Object invoked(Object obj,
												Object... args)
												throws Throwable {
											int index = readIndex();
											return imsis[index];
										}
									}, old);
						}

						// hook getSimSerialNumber
						Method getSimSerialNumber;
						try {
							getSimSerialNumber = clz.getMethod(
									"getSimSerialNumber", new Class<?>[0]);
						} catch (NoSuchMethodException e) {
							getSimSerialNumber = null;
						}
						if (getSimSerialNumber != null) {
							final MS.MethodPointer old = new MS.MethodPointer();

							MS.hookMethod(clz, getSimSerialNumber,
									new MS.MethodHook() {
										@Override
										public Object invoked(Object obj,
												Object... args)
												throws Throwable {
											int index = readIndex();
											return simseris[index];
										}
									}, old);
						}

					}
				});
	}
	
	static final String[] deviceIds = IMEI.data;
	static final String[] imsis = IMSI.data;
	static final String[] simseris = SER.data;
	static final String[] macs = MAC.data;
	static final String[] andIds = AndId.data;
	static final String[] brands = Brand.data;
	static final String[] models = Model.data;
}