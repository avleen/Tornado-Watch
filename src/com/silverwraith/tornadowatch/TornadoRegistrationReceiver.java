package com.silverwraith.tornadowatch;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.provider.Settings.Secure;
import android.util.Log;

public class TornadoRegistrationReceiver extends BroadcastReceiver {

	/* Heavily borrowed from:
	 * http://www.vogella.de/articles/AndroidCloudToDeviceMessaging/article.html
	 */

	public TornadoRegistrationReceiver() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		Log.w("C2DM", "Registration Receiver Called");
		String action = intent.getAction();
		if ("com.google.android.c2dm.intent.REGISTRATION".equals(action)) {
			Log.w("C2DM", "Received registration ID");
			String registrationId = intent.getStringExtra("registration_id");
			if (registrationId == null) {
				registrationId = "nokey";
			}
			String error = intent.getStringExtra("error");
			
			Log.d("C2DM", "dmControl: regisrationId = " + registrationId + ", error = " + error);
			String deviceId = Secure.getString(context.getContentResolver(), Secure.ANDROID_ID);
			// createNotification(context, registrationId);
			SendRegistrationID registration_id_sender = new SendRegistrationID();
			registration_id_sender.execute(deviceId, registrationId);
			// Also save it in the preference to be able to show it later
			saveRegistrationId(context, registrationId);
		}
	}
	
	private void saveRegistrationId(Context context, String registrationId) {
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(context);
		Editor edit = prefs.edit();
		edit.putString(TornadoWatchActivity.AUTH, registrationId);
		edit.commit();
	}

	public void createNotification(Context context, String registrationId) {
		NotificationManager notificationManager = (NotificationManager) context
				.getSystemService(Context.NOTIFICATION_SERVICE);
		Notification notification = new Notification(R.drawable.logo,
				"Registration successful", System.currentTimeMillis());
		// Hide the notification after its selected
		notification.flags |= Notification.FLAG_AUTO_CANCEL;

		Intent intent = new Intent(context, TornadoRegistrationResultActivity.class);
		intent.putExtra("registration_id", registrationId);
		PendingIntent pendingIntent = PendingIntent.getActivity(context, 0,
				intent, 0);
		notification.setLatestEventInfo(context, "Registration",
				"Successfully registered", pendingIntent);
		notificationManager.notify(0, notification);
	}
	
	class SendRegistrationID extends AsyncTask<String, Void, Void> {
		protected Void doInBackground(String... params) {
			Log.d("C2DM", "Sending registration ID to my application server");
			HttpParams httpParameters = new BasicHttpParams();
			int timeoutConnection = 3000;
			HttpConnectionParams.setConnectionTimeout(httpParameters, timeoutConnection);
			int timeoutSocket = 5000;
			HttpConnectionParams.setSoTimeout(httpParameters, timeoutSocket);
			HttpClient client = new DefaultHttpClient(httpParameters);
			HttpPost post = new HttpPost("http://tw.silverwraith.com/cgi-bin/register.py");

			while (true) {
				try {
					List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(1);
					// Get the deviceID
					nameValuePairs.add(new BasicNameValuePair("deviceid", params[0]));
					nameValuePairs.add(new BasicNameValuePair("registrationid", params[1]));

					post.setEntity(new UrlEncodedFormEntity(nameValuePairs));
					// Run this in an infinite loop, trying to connect to the server and register until it succeeds
					HttpResponse response = client.execute(post);
					BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
					String line = "";
					while ((line = rd.readLine()) != null) {
						Log.e("HttpResponse", line);
					}
				} catch (Exception e) {
					// If the registration fails, sleep 10 seconds and try again.
					e.printStackTrace();
					try {
						Thread.sleep(10000);
					} catch (InterruptedException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
					continue;
				}
				break;
			}
			httpParameters = null;
			post = null;
			client = null;
			return null;
		}
	}

}

