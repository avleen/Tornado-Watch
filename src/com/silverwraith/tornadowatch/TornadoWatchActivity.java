package com.silverwraith.tornadowatch;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapView;
import com.google.android.maps.MyLocationOverlay;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;
import com.google.android.maps.Projection;

import android.util.Log;
import android.view.GestureDetector.OnDoubleTapListener;
import android.view.GestureDetector.OnGestureListener;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;

import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings.Secure;
import android.widget.Toast;

public class TornadoWatchActivity extends MapActivity implements OnGestureListener, OnDoubleTapListener {
	
	public static final String AUTH = "authentication";
	MapView mapView;
	List<Overlay> mapOverlays;
	Drawable drawable;
	TornadoItemizedOverlay itemizedOverlay;
    MyLocationOverlay myLocationOverlay;
    Boolean initialLocation = false;
    String sID = null;
    static String installationFile = "INSTALLATION";
    static String TAG = "TW";
    String registrationId = null; 
    
	/** Called when the activity is first created. */
    @Override
    /** public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main); */
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    	setContentView(R.layout.main);
    	
    	// Do our registration dance first
    	class RegisterApp extends AsyncTask<String, Void, String> {
    		@Override
			protected String doInBackground(String... params) {
    			register();
    			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(TornadoWatchActivity.this);
    			String registrationId = prefs.getString(AUTH, "nokey");
    			Log.d("Tornado Debug", registrationId);
				return registrationId;
    		}
    		public void register() {
    			Log.w("C2DM", "start registration process");
    			Intent intent = new Intent("com.google.android.c2dm.intent.REGISTER");
    			intent.putExtra("app", PendingIntent.getBroadcast(TornadoWatchActivity.this, 0, new Intent(), 0));
    			intent.putExtra("sender", "avleen@gmail.com");
    			startService(intent);
    		}
    	}
    	RegisterApp do_registration = new RegisterApp();
    	do_registration.execute(new String[] { "" });
    	
    	// Start the LocationListener service
    	Log.i(TAG, "Starting location tracker service");
    	startService(new Intent(TornadoWatchActivity.this, TornadoWatchLocationLoggerService.class));
    	
    	mapView = (MapView) findViewById(R.id.mapView);
        mapView.setBuiltInZoomControls(true);
        mapView.setSatellite(false);
        
        /* A test marker, sits over Mexico City. Mmmm burrito.
        * mapOverlays = mapView.getOverlays();
        * drawable = this.getResources().getDrawable(R.drawable.androidmarker);
        * itemizedOverlay = new TornadoItemizedOverlay(drawable);
        *
        * GeoPoint point = new GeoPoint(19240000,-99120000);
        * OverlayItem overlayitem = new OverlayItem(point, "", "");
        * itemizedOverlay.addOverlay(overlayitem);
        * mapOverlays.add(itemizedOverlay);
        */
        
        /* Download markers. This should take just a moment, and gives the
         * location time to update too.
         */
        
        try {
			JSONArray json = downloadMarkers();
			mapOverlays = mapView.getOverlays();
			drawable = this.getResources().getDrawable(R.drawable.locationplace);
			itemizedOverlay = new TornadoItemizedOverlay(drawable);
			for (int i=0; i < json.length(); i++) {
				JSONObject json_data = json.getJSONObject(i);
	            GeoPoint point = new GeoPoint(json_data.getInt("lat"), json_data.getInt("lng"));
	            itemizedOverlay.addOverlay(new OverlayItem(point, "", ""));
	            mapOverlays.add(itemizedOverlay);
	            mapView.postInvalidate();
			}
		} catch (MalformedURLException e) {
			// Toast.makeText(TornadoWatchActivity.this, "Marker URL is malformed", Toast.LENGTH_SHORT).show();
			e.printStackTrace();
		} catch (JSONException e) {
			// Toast.makeText(TornadoWatchActivity.this, "Unable to decode markers", Toast.LENGTH_SHORT).show();
			e.printStackTrace();
		} catch (IOException e) {
			// Toast.makeText(TornadoWatchActivity.this, "Marker download failed", Toast.LENGTH_SHORT).show();
			e.printStackTrace();
		}
        
        /* Add an overlay for the current location marker.
         * Once we have it, refresh immediately and zoom to our location.
         */
        myLocationOverlay = new TornadoLocationOverlay(this, mapView);
        mapView.getOverlays().add(myLocationOverlay);
        mapView.postInvalidate();
    }
    
    @Override
    protected boolean isRouteDisplayed() {
    	return false;
    }

    protected void onResume() {
    	super.onResume();
    	final String deviceId = Secure.getString(this.getContentResolver(), Secure.ANDROID_ID);
    	// On start or resume, register for location updates!
    	myLocationOverlay.enableCompass();
    	myLocationOverlay.enableMyLocation();
    	myLocationOverlay.runOnFirstFix(new Runnable() {
            public void run() {
                mapView.getController().animateTo(myLocationOverlay.getMyLocation());
                Log.i(TAG, myLocationOverlay.getMyLocation().toString());
                int myLong = myLocationOverlay.getMyLocation().getLongitudeE6();
                int myLat = myLocationOverlay.getMyLocation().getLatitudeE6();
        		HttpClient client = new DefaultHttpClient();
        		HttpPost post = new HttpPost("http://tw.silverwraith.com/cgi-bin/updatelocation.py");
        		// Get the current registrationId. This might be "nokey"!
        		registrationId = showRegistrationId();
        		if (registrationId != null) {        		
        			try {
        				List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(1);
        				nameValuePairs.add(new BasicNameValuePair("long", String.valueOf(myLong)));
        				nameValuePairs.add(new BasicNameValuePair("lat", String.valueOf(myLat)));
        				nameValuePairs.add(new BasicNameValuePair("registrationId", registrationId));
        				nameValuePairs.add(new BasicNameValuePair("deviceId", deviceId));
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
        });
    }

	protected void onPause() {
    	super.onPause();
    	/* Be polite - when we're closed, don't get network location updates.
    	 * TODO(avleen): This will change once we can run in the background,
    	 * to warn of nearby weather changes.
    	 */
    	myLocationOverlay.disableMyLocation();
    }
	public boolean onDown(MotionEvent arg0) {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean onFling(MotionEvent arg0, MotionEvent arg1, float arg2,
			float arg3) {
		// TODO Auto-generated method stub
		return false;
	}

	public void onLongPress(MotionEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	public boolean onScroll(MotionEvent arg0, MotionEvent arg1, float arg2,
			float arg3) {
		// TODO Auto-generated method stub
		return false;
	}

	public void onShowPress(MotionEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	public boolean onSingleTapUp(MotionEvent arg0) {
		return true;
	}

	public boolean onDoubleTap(MotionEvent arg0) {
		int x = (int)arg0.getX();
		int y = (int)arg0.getY();
		Projection p = mapView.getProjection();
		mapView.getController().animateTo(p.fromPixels(x, y));
		mapView.getController().zoomIn();
		return true;
	}

	public boolean onDoubleTapEvent(MotionEvent arg0) {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean onSingleTapConfirmed(MotionEvent arg0) {
		// TODO Auto-generated method stub
		return false;
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.optionsmenu, menu);
	    return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    // Handle item selection
	    switch (item.getItemId()) {
	        case R.id.my_location:
	            zoomToMyLocation(false);
	            return true;
	        case R.id.place_marker:
	        	placeMarker();
	        	return true;
	        case R.id.preferences:
	        	startActivity(new Intent("com.silverwraith.tornadowatch.TornadoPreferenceActivity"));
	        default:
	            return super.onOptionsItemSelected(item);
	    }
	}

	private void zoomToMyLocation(Boolean initialLocation) {
		GeoPoint myLocationGeoPoint = myLocationOverlay.getMyLocation();
		if (myLocationGeoPoint != null) {
			mapView.getController().animateTo(myLocationGeoPoint);
			mapView.getController().setZoom(10);
		} else {
			if (initialLocation == false) {
				Toast.makeText(this, "Cannot determine location", Toast.LENGTH_SHORT).show();
			}
		}
	}
	
	private void placeMarker() {		
		mapOverlays = mapView.getOverlays();
		drawable = this.getResources().getDrawable(R.drawable.locationplace);
		itemizedOverlay = new TornadoItemizedOverlay(drawable);
        GeoPoint point = myLocationOverlay.getMyLocation();
        itemizedOverlay.addOverlay(new OverlayItem(point, "", ""));
        mapOverlays.add(itemizedOverlay);
        mapView.postInvalidate();
        
        try {
			submitCoordinates(point.getLongitudeE6(), point.getLatitudeE6(), sID);
		} catch (MalformedURLException e) {
			Toast.makeText(this, "Malformed URL", Toast.LENGTH_SHORT).show();
		}
	}
	
	public void submitCoordinates(int lng, int lat, String sID) throws MalformedURLException {
		String queryString = "?lng=" + lng + "&lat=" + lat + "&uid=" + sID;
		URL url = new URL("http://silverwraith.com/tw_submit.php" + queryString);
		try {
			URLConnection urlConnection = url.openConnection();
			InputStream in = new BufferedInputStream(urlConnection.getInputStream());
			InputStreamReader is = new InputStreamReader(in);
			BufferedReader br = new BufferedReader(is);
			String read = br.readLine();

			while(read != null) {
			    read = br.readLine();
			}
		} catch (IOException e) {
			Toast.makeText(this, "Unable to submit URL - try again!", Toast.LENGTH_SHORT).show();
		}
	}
	
	public static String downloadJSON() throws MalformedURLException, IOException {
		String url = "http://silverwraith.com/tw.json";
		InputStream in = new URL(url).openStream();
		InputStreamReader is = new InputStreamReader(in);
		StringBuilder total = new StringBuilder();
		String jsonLine;
		BufferedReader br = new BufferedReader(is);
		
		while ((jsonLine = br.readLine()) != null) {
			total.append(jsonLine);
		}
		in.close();
		return total.toString();
	}

	public static JSONArray downloadMarkers() throws MalformedURLException, JSONException, IOException {
		JSONArray json = new JSONArray(downloadJSON());
		return json;
	}

	public String showRegistrationId() {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		String registrationId = prefs.getString(AUTH, null);
		Log.d("C2DM RegId requested", registrationId);
		return registrationId;

	}
}