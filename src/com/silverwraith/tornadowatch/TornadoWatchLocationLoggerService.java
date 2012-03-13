package com.silverwraith.tornadowatch;

import java.io.BufferedReader;
import java.io.IOException;
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

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

public class TornadoWatchLocationLoggerService extends Service implements LocationListener {

	private final static String TAG = "TW";
	public static final long DELAY = 3600000; // 60 min
	public static final String AUTH = "authentication";
	private String registrationId = null;
	
	public TornadoWatchLocationLoggerService() {
		// TODO Auto-generated constructor stub
	}
	
    public class LocalBinder extends Binder {
    	TornadoWatchLocationLoggerService getService() {
            return TornadoWatchLocationLoggerService.this;
        }
    }

    @Override
    public void onCreate() {
    	super.onCreate();
		Log.i(TAG, "Location manager service created");
	}

    public void onStart(Intent intent, int startId) {
    	super.onStart(intent, startId);
    	Log.i(TAG, "Location logger service started");
    }
    
	public int onStartCommand(Intent intent, int flags, int startId) {
		// TODO Auto-generated method stub
		Log.w(TAG, "Location logger service started");
		return START_STICKY;
	}
	@Override public void onDestroy() {
		super.onDestroy();
		Log.w(TAG, "Location logger service destroyed");
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	public void onLocationChanged(Location loc) {
	   Log.d(TAG, "onLocationChanged with Location: " + loc.toString());
	   HttpClient client = new DefaultHttpClient();
	   HttpPost post = new HttpPost("http://tw.silverwraith.com/cgi-bin/updatelocation.py");
	   // Get the current registrationId. This might be "nokey"!
	   registrationId = showRegistrationId();
	   double myLong = loc.getLongitude();
	   double myLat = loc.getLatitude();
	   if (registrationId != null) {        		
		  try {
			  List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(1);
			  nameValuePairs.add(new BasicNameValuePair("long", String.valueOf(myLong)));
			  nameValuePairs.add(new BasicNameValuePair("lat", String.valueOf(myLat)));
			  nameValuePairs.add(new BasicNameValuePair("registrationId", registrationId));
			  post.setEntity(new UrlEncodedFormEntity(nameValuePairs));
			  HttpResponse response = client.execute(post);
			  BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
			  String line = "";
			  while ((line = rd.readLine()) != null) {
				  Log.e("HttpResponse", line);
			  }
		  } catch (IOException e) {
			  e.printStackTrace();
		  }
	   }
	}
	
	public void onProviderDisabled(String arg0) {
		// TODO Auto-generated method stub
		
	}

	public void onProviderEnabled(String arg0) {
		// TODO Auto-generated method stub
		
	}

	public void onStatusChanged(String arg0, int arg1, Bundle arg2) {
		// TODO Auto-generated method stub
		
	}
	
	public String showRegistrationId() {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		String registrationId = prefs.getString(AUTH, null);
		Log.d("C2DM RegId requested", registrationId);
		return registrationId;

	}

}