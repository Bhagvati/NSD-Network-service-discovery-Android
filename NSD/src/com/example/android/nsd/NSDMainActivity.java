package com.example.android.nsd;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.List;

import org.apache.http.conn.util.InetAddressUtils;

import android.app.Activity;
import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.StrictMode;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.example.android.nsdchat.R;

public class NSDMainActivity extends Activity {

	NsdHelper mNsdHelper;
	public TextView mStatusView;
	private Handler handler = new Handler();
	public static final String TAG = "Nexus_Phone";
	public File file;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		mStatusView = (TextView) findViewById(R.id.status);
		mNsdHelper = new NsdHelper(this);
		mNsdHelper.initializeNsd();
		StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder()
				.permitAll().build();
		StrictMode.setThreadPolicy(policy);

	}


	public void clickAdvertise(View v) {
		handler.post(new Runnable() {

			@Override
			public void run() {
				try {
					String iNetAddress = getIPAddress(true);
					InetAddress inetAddress = InetAddress
							.getByName(iNetAddress);
					Log.e(TAG, "ip " + iNetAddress);
					mNsdHelper.registerService(getport(), inetAddress);
					Log.e("NSD", "Registered");
					Log.e("inetaddress", inetAddress.toString());
				} catch (UnknownHostException e) {
					e.printStackTrace();
				}
			}
		});

	}

	public int getport() {

		try {
			ServerSocket mServerSocket;
			mServerSocket = new ServerSocket(0);
			return mServerSocket.getLocalPort();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return 123;
		}

	}

	public static String getIPAddress(boolean useIPv4) {
		try {
			List<NetworkInterface> interfaces = Collections
					.list(NetworkInterface.getNetworkInterfaces());
			for (NetworkInterface intf : interfaces) {
				List<InetAddress> addrs = Collections.list(intf
						.getInetAddresses());
				for (InetAddress addr : addrs) {
					if (!addr.isLoopbackAddress()) {
						String sAddr = addr.getHostAddress().toUpperCase();
						boolean isIPv4 = InetAddressUtils.isIPv4Address(sAddr);
						if (useIPv4) {
							if (isIPv4)
								return sAddr;
						} else {
							if (!isIPv4) {
								int delim = sAddr.indexOf('%'); // drop ip6 port
																// suffix
								return delim < 0 ? sAddr : sAddr.substring(0,
										delim);
							}
						}
					}
				}
			}
		} catch (Exception ex) {
		} 
		return "";
	}

	public void clickDiscover(View v) {
		mNsdHelper.discoverServices();
		mStatusView.setText("");

	}

	public void listService(final NsdServiceInfo service) {
		handler.postDelayed(new Runnable() {
			@SuppressWarnings("static-access")
			@Override
			public void run() {

				String msg = "name :" + service.getServiceName()
						+ "\n port: " + service.getPort();

				InetAddress host = service.getHost();
				if (host != null) {
					msg = msg + "\n ip: " + ""
							+ service.getHost().getHostAddress()
							+ "\n canonical host name : " + ""
							+ service.getHost().getCanonicalHostName()
							+ "\n host name: " + ""
							+ service.getHost().getHostName();
				} else {
					msg = msg + "host is null";
				}
				msg = msg + "\n============== " + "\n "
						+ mStatusView.getText();
				mStatusView.setText(msg);

			}
		}, 100);
	}

	@Override
	protected void onPause() {
		super.onPause();
	}

	@Override
	protected void onResume() {
		super.onResume();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	public class NsdHelper {

		Context mContext;

		NsdManager mNsdManager;
		NsdManager.ResolveListener mResolveListener;
		NsdManager.DiscoveryListener mDiscoveryListener;
		NsdManager.RegistrationListener mRegistrationListener;

		public static final String SERVICE_TYPE ="_http._tcp."; //use your service type you want to use
		public static final String TAG = "NsdHelper";
		public String mServiceName = getDeviceName() + "";
		NsdServiceInfo mService;

		public NsdHelper(Context context) {
			mContext = context;
			mNsdManager = (NsdManager) context
					.getSystemService(Context.NSD_SERVICE);
		}

		public String getDeviceName() {
			String manufacturer = Build.MANUFACTURER;
			String model = Build.MODEL;
			int version = Build.VERSION.SDK_INT;
			String filename = model + "_" + version;

			if (model.startsWith(manufacturer)) {
				return capitalize(model);
			} else {
				return capitalize(manufacturer) + " " + model;
			}
		}

		public String getDeviceVersion() {
			String manufacturer = Build.MANUFACTURER;
			String model = Build.MODEL;
			int version = Build.VERSION.SDK_INT;
			String filename = model + "_" + version;

			if (model.startsWith(manufacturer)) {
				return capitalize(model) + "_" + filename;
			} else {
				return capitalize(manufacturer) + "_" + model + " " + filename;
			}
		}

		private String capitalize(String s) {
			if (s == null || s.length() == 0) {
				return "";
			}
			char first = s.charAt(0);
			if (Character.isUpperCase(first)) {
				return s;
			} else {
				return Character.toUpperCase(first) + s.substring(1);
			}
		}

		public void initializeNsd() {
			initializeResolveListener();
			initializeDiscoveryListener();
			initializeRegistrationListener();

		}

		public void initializeDiscoveryListener() {
			mDiscoveryListener = new NsdManager.DiscoveryListener() {

				@Override
				public void onDiscoveryStarted(String regType) {
					Log.d(TAG, "Service discovery started");
				}

				@Override
				public void onServiceFound(NsdServiceInfo service) {
					if (service.getServiceType().contains("luxul")) {
						mNsdManager.resolveService(service, mResolveListener);
						Log.e(TAG, "service info :: " + service + "..");
						mService = service;
					}
				}

				@Override
				public void onServiceLost(NsdServiceInfo service) {
					Log.e(TAG, "service lost" + service);
					if (mService == service) {
						mService = null;
					}
				}

				@Override
				public void onDiscoveryStopped(String serviceType) {
					Log.i(TAG, "Discovery stopped: " + serviceType);
				}

				@Override
				public void onStartDiscoveryFailed(String serviceType,
						int errorCode) {
					Log.e(TAG, "Discovery failed: Error code:" + errorCode);
					mNsdManager.stopServiceDiscovery(this);
				}

				@Override
				public void onStopDiscoveryFailed(String serviceType,
						int errorCode) {
					Log.e(TAG, "Discovery failed: Error code:" + errorCode);
					mNsdManager.stopServiceDiscovery(this);
				}
			};
		}

		public void initializeResolveListener() {
			mResolveListener = new NsdManager.ResolveListener() {

				@Override
				public void onResolveFailed(NsdServiceInfo serviceInfo,
						int errorCode) {
					Log.e(TAG, "Resolve failed" + errorCode);
				}

				@Override
				public void onServiceResolved(NsdServiceInfo serviceInfo) {
					Log.e(TAG, "host :  "
							+ serviceInfo.getHost().getHostAddress());
					Log.e(TAG, "Address :  "
							+ serviceInfo.getHost().getAddress());
					listService(serviceInfo);
					if (serviceInfo.getServiceName().equals(mServiceName)) {
						Log.d(TAG, "Same IP.");
						return;
					}
					mService = serviceInfo;
				}
			};
		}

		public void initializeRegistrationListener() {
			mRegistrationListener = new NsdManager.RegistrationListener() {

				@Override
				public void onServiceRegistered(NsdServiceInfo NsdServiceInfo) {
					mServiceName = NsdServiceInfo.getServiceName();
					Toast.makeText(mContext,
							"device registerd  :" + mServiceName, 1000).show();
				}

				@Override
				public void onRegistrationFailed(NsdServiceInfo arg0, int arg1) {
				}

				@Override
				public void onServiceUnregistered(NsdServiceInfo arg0) {
				}

				@Override
				public void onUnregistrationFailed(NsdServiceInfo serviceInfo,
						int errorCode) {
				}

			};
		}

		public void registerService(int port, InetAddress ip) {
			NsdServiceInfo serviceInfo = new NsdServiceInfo();
			serviceInfo.setPort(port);
			serviceInfo.setServiceName(mServiceName);
			serviceInfo.setServiceType(SERVICE_TYPE);
			serviceInfo.setHost(ip);

			mNsdManager.registerService(serviceInfo,
					NsdManager.PROTOCOL_DNS_SD, mRegistrationListener);

		}

		public NsdServiceInfo discoverServices() {
			mNsdManager.discoverServices(SERVICE_TYPE,
					NsdManager.PROTOCOL_DNS_SD, mDiscoveryListener);
			return mService;
		}

		public void stopDiscovery() {
			mNsdManager.stopServiceDiscovery(mDiscoveryListener);
		}

		public NsdServiceInfo getChosenServiceInfo() {
			return mService;
		}

		public void tearDown() {
		}

	}
}
