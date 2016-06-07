package org.android.cydiahook;

import android.app.Activity;
import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.provider.Settings.Secure;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

public class MainActivity extends Activity {

	TextView resText;
	static final String key = "sys.rw.Deviceindex";

	private String initMAC() {
		WifiManager localWifiManager = (WifiManager) getSystemService("wifi");
		if (localWifiManager.getConnectionInfo().getMacAddress() != null) {
			return localWifiManager.getConnectionInfo().getMacAddress();
		}
		return null;

	}
	
	private int readIndex() {
		String index = PropertyUtil.read(key);
		int in;
		try {
			in = Integer.parseInt(index);
		} catch (NumberFormatException e) {
			return 0;
		}
		return in;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		resText = (TextView) findViewById(R.id.textView2);

		findViewById(R.id.button1).setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
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
				Log.d("hook", text);
				
//				try {
//					Class<?> clz = Class.forName("android.provider.Settings$Secure");
//					Method methodGetAndroidId;
//					try {
//						methodGetAndroidId = clz.getMethod("getString",
//								ContentResolver.class, String.class);
//					} catch (NoSuchMethodException e) {
//						methodGetAndroidId = null;
//					}
//					if(methodGetAndroidId!=null){
//						Object obj = methodGetAndroidId.invoke(null, getContentResolver(),"android_id");
//						String ssd = String.valueOf(obj);
//						Log.d("dddd", ssd);
//					}
//				} catch (ClassNotFoundException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				} catch (IllegalArgumentException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				} catch (IllegalAccessException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				} catch (InvocationTargetException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
			}
		});

		findViewById(R.id.button2).setOnClickListener(new OnClickListener() {

		

			@Override
			public void onClick(View v) {
				int in = readIndex();
				Log.d("cydCrash", "read index  " + in);
				in++;
				if (in >= Main.deviceIds.length) {
					in = 0; // reset
				}
				Log.d("cydCrash", "change index to " + in);
				PropertyUtil.set(key, String.valueOf(in));

				Log.d("cydCrash", "check index " + PropertyUtil.read(key));
			}
		});
	}

}
