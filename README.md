# hook android api伪造设备信息做刷量
------

## 概述

Android平台上app的统计数据都是基于Android的设备信息的，比如首次使用（激活），活跃（日活跃DAU，月活跃MAU）等都需要根据设备唯一信息来统计。不仅统计，业务需求也都是根据设备唯一信息来展开的，比如签到，同一台设备不能签第二次，比如运营推广发积分发红包，每台手机只能领取一次，再比如app分发渠道统计应用的激活，同一台手机第一次使用某app才算激活等等这些场景都需要靠设备信息来唯一确定一台手机。

怎么唯一确定一台Android手机，Android程序员都知道，
> * IMEI
> * IMSI
> * SIM_SER sim卡序列号
> * MAC地址，
> * Android_ID android系统第一次启动生成的一个唯一ID
> * Brand 手机品牌
> * Model 手机型号

这些信息基本能唯一定位一台手机，为了便于使用，我们的设备ID一般取其中的部分字段做hash，比如hash(MAC地址+IMEI)。

有些刷量的工具依赖于虚拟机，开发虚拟机或者修改android模拟器来模拟不同的硬件设备。从分层角度上看，android系统从下到上可以抽象为这几层 ：设备硬件层，应用框架层API，和应用APP，其中设备+API 两层就可以看做是实现了虚拟机。
应用层App直接与API打交道，相同的api调用，不同的返回就相当于不同的虚拟机了。

> * **APP**
> * `API`
> * `硬件设备`


## hook简介

Cydia Substrate hook框架可以hook Java和 C native层的代码，运行时改变代码的实现，是一个很强大的hook工具，也能做为很强大的调试工具。

官网地址：http://www.cydiasubstrate.com/

SDK下载地址：http://asdk.cydiasubstrate.com/zips/cydia_substrate-r2.zip

demo地址：https://github.com/zencodex/cydia-android-hook.git

hook框架：https://cache.saurik.com/apks/com.saurik.substrate_0.9.4010.apk


### Cydia Substrate的使用：
1. 前提，手机需要root权限
2. 手机上安装hook框架apk
3. 参考demo编写hook的源程序
4. 使用hook框架程序link，重启手机

cydia substrate的使用可以参考这两篇文章

[http://www.csdn.net/article/2015-08-07/2825405](http://www.csdn.net/article/2015-08-07/2825405)
[http://drops.wooyun.org/tips/8084](http://drops.wooyun.org/tips/8084)

## 设备信息API的hook
### 获取imei
Java 代码

	public String getIMEI() {
		String imei = "";
		TelephonyManager telepManager;
		telepManager = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
		imei = telepManager.getDeviceId();
		return imei;
	}

获取IMEI调用api是 android.telephony.TelephonyManager.getDeviceId()，
如何hook呢

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

				});
	}

这样，被hook的这台android设备上调用TelephonyManager.getDeviceId()返回的就不再是设备的真实IMEI，而是deviceIds[index]，deviceIds是一个设备IMEI号的数组，我们构造一个样本足够大deviceIds，就容易做到了IMEI号的伪造和刷量。

### 获取imsi / sim卡序列号
android.telephony.TelephonyManager.getSubscriberId();

android.telephony.TelephonyManager.getSimSerialNumber();

imsi和sim卡序列号的hook方法与 imei的hook类似，不再重复。
### mac地址
	public String getMacAddress() {
		WifiManager wifi = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
		WifiInfo info = wifi.getConnectionInfo();
		return info.getMacAddress();
	}

android.net.wifi.WifiInfo.getMacAddress();

getMacAddress()的hook与IMEI的写法基本一样，替换一下类名和方法名就可，
不再重复。
### android_ID
				// Secure.ANDROID_ID : public static final String ANDROID_ID = "android_id";
		return android.provider.Settings.System.getString(mContext.getContentResolver(),Secure.ANDROID_ID);

android_id 的hook方式

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
				

 Object invoked(Object obj,Object... args) 中 obj是指实例化的类对象，在当前这个场景下就是Secure的对象，args是obj.getString（ContentResolver，String）的参数列表，依次是ContentResolver对象和String类型的key，下标从0开始。args[0]是ContentResolver类型，args[1]是String类型，当args[1] == “android_id”时，也就是调用getString（ContentResolver，“android_id”）时，返回值被篡改，key为别的字符串时，按照原本的逻辑返回应有的属性
 
 	return old.invoke(obj, args);
### brand 和 model
	android.os.Build.BRAND; // brand
	android.os.Build.MODEL; // model;
	
	
brand和model是在Build类里定义的两个静态变量，没发现cydia substrate针对类变量的修改机制，这个hook怎么做呢？请往下看:
	
    /** The brand (e.g., carrier) the software is customized for, if any. */
    public static final String BRAND = getString("ro.product.brand");

    /** The end-user-visible name for the end product. */
    public static final String MODEL = getString("ro.product.model");
	
注意到Brand和MODEL两个变量初始化时调用了getString方法，hook住这个方法，根据传人的参数做好判断应该就可以了。继续往下看：

	  private static String getString(String property) {
        return SystemProperties.get(property, UNKNOWN);
    }
    
这个方法是是private的，通过反射去getMethod时候麻烦一下，我们继续往下走一步，来到了android.os.SystemProperties.get(String,String),于是乎，我们hook这个api。

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
## 源码和demo
验证的测试代码

	TelephonyManager telepManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
				String imei = telepManager.getDeviceId();
				String andId = Secure.getString(getContentResolver(),
						"android_id");
				String mac = initMAC();

				String imsi = telepManager.getSubscriberId();
				String ss = telepManager.getSimSerialNumber();
				String brand = android.os.Build.BRAND;
				String model = android.os.Build.MODEL;
				String text = String.format("%s,%s,%s,%s,%s,%s,%s,---%s", imei,
						andId, mac, imsi, ss,brand,model,String.valueOf(readIndex()));
				resText.setText(text);
### hook前的设备真实数据
resText显示 ：null,6fdace0b66e3cac,null,,,FZS,FZS_Y80_NB,---30

### hook后的伪造数据
resText显示 ：865761021428959,ab591f34ef3b9aa6,5c:f7:c3:33:b3:0c,460012538363135,606390041315,GiONEE,W900,---30

只要设备信息的数据空间足够大，就可以创造很多的DAU，很多的激活。。。

当然，很多平台都有自己的防作弊系统，比如会分析IP是否离散等等，单纯修改设备信息可能不一定会有效果。

### hook程序的源代码和测试demo
[https://github.com/devxiaobai/android_device_info_hook.git](https://github.com/devxiaobai/android_device_info_hook.git)

##参考

[http://www.csdn.net/article/2015-08-07/2825405](http://www.csdn.net/article/2015-08-07/2825405)
[http://drops.wooyun.org/tips/8084](http://drops.wooyun.org/tips/8084)

