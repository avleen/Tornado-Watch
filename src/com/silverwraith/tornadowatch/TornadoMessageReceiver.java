package com.silverwraith.tornadowatch;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

public class TornadoMessageReceiver extends BroadcastReceiver {

	public TornadoMessageReceiver() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		// TODO Auto-generated method stub
		String action = intent.getAction();
		Log.w("C2DM", "Message Receiver called");
		if ("com.google.android.c2dm.intent.RECEIVE".equals(action)) {
			Log.w("C2DM", "Received message");
			final String payload = intent.getStringExtra("payload");
			Log.d("C2DM", "dmControl: payload = " + payload);
			// TODO Send this to my application server to get the real data
			// Lets make something visible to show that we received the message
			createNotification(context, payload);
		}
	}
	
	public void createNotification(Context context, String payload) {
		// Get notification preferences
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		boolean high_priority_play_sound = prefs.getBoolean("high_priority_play_sound", true);
		boolean high_priority_vibrate = prefs.getBoolean("high_priority_vibrate", true);
				
		NotificationManager notificationManager = (NotificationManager) context
				.getSystemService(Context.NOTIFICATION_SERVICE);
		Notification notification = new Notification(R.drawable.logo,
				"New tornados reported in your area", System.currentTimeMillis());
		// Hide the notification after its selected
		notification.flags |= Notification.FLAG_AUTO_CANCEL;
		if (high_priority_play_sound == true) {
			notification.defaults |= Notification.DEFAULT_SOUND;
		}
		if (high_priority_vibrate == true) {
			notification.defaults |= Notification.DEFAULT_VIBRATE;
		}

		if (payload != null && payload.length() > 0) {
			Intent intent = new Intent(context, TornadoWatchActivity.class);
			intent.putExtra("payload", payload);
			PendingIntent pendingIntent = PendingIntent.getActivity(context, 0,
				intent, 0);
			notification.setLatestEventInfo(context, "Tornado Watch",
				"New tornado in your area!", pendingIntent);
			notificationManager.notify(0, notification);
		}
	}

}
