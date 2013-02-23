package com.silverwraith.tornadowatch;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
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
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
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
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.ads.AdRequest;
import com.google.ads.AdSize;
import com.google.ads.AdView;

public class TornadoWatchActivity extends MapActivity implements OnGestureListener, OnDoubleTapListener {
	
	public static final String AUTH = "authentication";
	MapView mapView;
	List<Overlay> mapOverlays;
	Drawable drawableLow;
	Drawable drawableHigh;
	TornadoItemizedOverlay itemizedOverlayLow;
	TornadoItemizedOverlay itemizedOverlayHigh;
    static MyLocationOverlay myLocationOverlay;
    Boolean initialLocation = false;
    static String installationFile = "INSTALLATION";
    static String TAG = "TW";
    String registrationId = null; 
    static String CGI_BASE = "http://tw.silverwraith.com/cgi-bin";
    private AdView adView;
    
	/** Called when the activity is first created. */
    @Override
    /** public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main); */
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    	try {
			if(isDebugBuild()) {
				setContentView(R.layout.main_debug);
			} else {
				setContentView(R.layout.main);
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	
    	// Before anything else, make sure we have a network connection
    	final ConnectivityManager conMgr =  (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
    	final NetworkInfo activeNetwork = conMgr.getActiveNetworkInfo();
    	if (activeNetwork != null && activeNetwork.isConnected()) {
    	    //notify user you are online
    	} else {
    		Toast.makeText(TornadoWatchActivity.this, "No internet connection found!", Toast.LENGTH_SHORT).show();
    		this.finish();
    	}
    	
    	// Do our registration dance
    	class RegisterApp extends AsyncTask<Void, Void, Void> {
    		protected Void doInBackground(Void... arg0) {
    			Log.w("C2DM", "start registration process");
    			Intent intent = new Intent("com.google.android.c2dm.intent.REGISTER");
    			intent.putExtra("app", PendingIntent.getBroadcast(TornadoWatchActivity.this, 0, new Intent(), 0));
    			intent.putExtra("sender", "avleen@gmail.com");
    			startService(intent);
    			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(TornadoWatchActivity.this);
    			String registrationId = prefs.getString(AUTH, "nokey");
    			Log.d("Tornado Debug", registrationId);
				return null;
    		}
    		
			@SuppressWarnings("unused")
			protected void onPostExecute(Void... arg0) {
				super.onPostExecute(null);
    			Toast.makeText(TornadoWatchActivity.this, "Successfully registered with Tornado Alert", Toast.LENGTH_SHORT).show();
    		}
    	}
    	Toast.makeText(TornadoWatchActivity.this, "Registering with Tornado Alert service...", Toast.LENGTH_SHORT).show();
    	RegisterApp do_registration = new RegisterApp();
    	do_registration.execute();
    	
    	// Start the LocationListener service
    	Log.i(TAG, "Starting location tracker service");
    	startService(new Intent(TornadoWatchActivity.this, TornadoWatchLocationLoggerService.class));
    	
    	mapView = (MapView) findViewById(R.id.mapView);
        mapView.setBuiltInZoomControls(true);
        mapView.setSatellite(false);
        myLocationOverlay = new TornadoLocationOverlay(TornadoWatchActivity.this, mapView);
        
        // Let the user know that we're updating the tornado markers
        Toast.makeText(TornadoWatchActivity.this, "Updating tornado reports...", Toast.LENGTH_SHORT).show();
        GetAndDrawMarkers do_get_draw_markers = new GetAndDrawMarkers();
        do_get_draw_markers.execute("onCreate");
        do_get_draw_markers = null;
        
    	// Ads!
    	// Look up the AdView as a resource and load a request.
    	adView = new AdView(this, AdSize.BANNER, "a15126d73db75bf");
        // Lookup your LinearLayout assuming it's been given
        // the attribute android:id="@+id/mainLayout"
        LinearLayout layout = (LinearLayout)findViewById(R.id.zoom);
        layout.addView(adView);
        // Initiate a generic request to load it with an ad
        adView.loadAd(new AdRequest());
    }
    
    @Override
    public void onDestroy() {
      if (adView != null) {
        adView.destroy();
      }
      super.onDestroy();
    }
    
    public boolean isDebugBuild() throws Exception {
       PackageManager pm = this.getPackageManager();
       PackageInfo pi = pm.getPackageInfo(this.getPackageName(), 0);
       return ((pi.applicationInfo.flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0);
    }

	class GetAndDrawMarkers extends AsyncTask<String, Void, String> {
		String msg;
		@Override
		protected String doInBackground(String... params) {
			// Download markers in the background		       
			try {
		       	Log.i(TAG, "About to download markers");
				JSONArray json = downloadMarkers();
				mapOverlays = mapView.getOverlays();
				drawableLow = TornadoWatchActivity.this.getResources().getDrawable(R.drawable.locationplace);
				drawableHigh = TornadoWatchActivity.this.getResources().getDrawable(R.drawable.locationplacered);
				itemizedOverlayLow = new TornadoItemizedOverlay(drawableLow);
				itemizedOverlayHigh = new TornadoItemizedOverlay(drawableHigh);
				Log.i(TAG, "Number of markers found: " + json.length());
				// Save the old overlays so we can compare them later
				List<Overlay> oldOverlays = mapView.getOverlays();
				if (json.length() > 0) {
					if (!mapOverlays.isEmpty()) {
						mapOverlays.clear();
					}
					for (int i=0; i < json.length(); i++) {
						JSONObject json_data = json.getJSONObject(i);
						float markerLat = Float.valueOf(json_data.getString("lat")) * 1000000;
						float markerLng = Float.valueOf(json_data.getString("lng")) * 1000000;
						String markerPri = json_data.getString("priority");
						Log.i(TAG, "Marker: " + markerLat + "," + markerLng + "," + markerPri );
		            	GeoPoint point = new GeoPoint((int)markerLat, (int)markerLng);
		            	if (json_data.getString("priority") == "true") {
		            		itemizedOverlayHigh.addOverlay(new OverlayItem(point, "", ""));
		            		mapOverlays.add(itemizedOverlayHigh);
		            	} else {
		            		itemizedOverlayLow.addOverlay(new OverlayItem(point, "", ""));
		            		mapOverlays.add(itemizedOverlayLow);
		            	}
					}
				}
				if (params[0] == "onCreate") {
			        msg = "Updating tornado reports... done!";
				} else if (mapOverlays != oldOverlays) {
					Log.i(TAG, "New markers found!");
					mapView.postInvalidate();
					//createNotification(TornadoWatchActivity.this, "");
					msg = "Tornado report markers updated";
				} else {
					msg = "No change in tornado reports";
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
	        mapView.getOverlays().add(myLocationOverlay);
	        mapView.postInvalidate();
	        return msg;
		}
		
		protected void onPostExecute(String msg) {
			Toast.makeText(TornadoWatchActivity.this, msg, Toast.LENGTH_SHORT).show();
		}
	}
    
    @Override
    protected boolean isRouteDisplayed() {
    	return false;
    }

    protected void onResume() {
    	super.onResume();
    	final String deviceId = Secure.getString(this.getContentResolver(), Secure.ANDROID_ID);
    	// On start or resume, register for location updates!
    	// myLocationOverlay.enableCompass();
    	myLocationOverlay.enableMyLocation();
    	// On some phones, runOnFirstFix() runs before there's a location fix. so getMyLocation() returns NULL.
    	// Catch this.
    	myLocationOverlay.runOnFirstFix(new Runnable() {
    		public void run() {
    			if (myLocationOverlay.getMyLocation() != null) {
    				mapView.getController().animateTo(myLocationOverlay.getMyLocation());
    				mapView.getController().setZoom(10);
    				Log.i(TAG, myLocationOverlay.getMyLocation().toString());
    				int myLng = myLocationOverlay.getMyLocation().getLongitudeE6();
    				int myLat = myLocationOverlay.getMyLocation().getLatitudeE6();
    				// Get the current registrationId. This might be "nokey"!
    				registrationId = showRegistrationId();
    				if (registrationId != null) {        		
    					AsyncSubmitInitialLocation do_submit_initial_location = new AsyncSubmitInitialLocation();
    					do_submit_initial_location.execute(String.valueOf(myLng), String.valueOf(myLat), registrationId, deviceId);
    				}
    			}
    		}
    	});
    }
	class AsyncSubmitInitialLocation extends AsyncTask<String, Void, Void> {
		String screenmsg = null;
		URI url;
		protected Void doInBackground(String... param) {
			try {
				url = new URI(CGI_BASE + "/updatelocation.py");
			} catch (URISyntaxException e) {
				e.printStackTrace();
			}
			try {
				// Set some timeouts for the HTTP connection
				HttpParams httpParameters = new BasicHttpParams();
				int timeoutConnection = 3000;
				HttpConnectionParams.setConnectionTimeout(httpParameters, timeoutConnection);
				int timeoutSocket = 5000;
				HttpConnectionParams.setSoTimeout(httpParameters, timeoutSocket);
				HttpClient client = new DefaultHttpClient(httpParameters);				
				HttpPost post = new HttpPost(url);
				List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(1);
				nameValuePairs.add(new BasicNameValuePair("lng", param[0]));
				nameValuePairs.add(new BasicNameValuePair("lat", param[1]));
				nameValuePairs.add(new BasicNameValuePair("registrationId", param[2]));
				nameValuePairs.add(new BasicNameValuePair("deviceId", param[3]));
				post.setEntity(new UrlEncodedFormEntity(nameValuePairs));
				HttpResponse response = client.execute(post);
				BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
				String line = "";
				while ((line = rd.readLine()) != null) {
					Log.e("HttpResponse", line);
				}
				httpParameters = null;
				post = null;
				client = null;
			} catch (IOException e) {
				screenmsg = "Unable to submit marker - try again!";
			}
			return null;
		}
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
	        case R.id.get_markers:
	        	// getAndDrawMarkers("UserRefresh");
	        	GetAndDrawMarkers do_get_draw_markers = new GetAndDrawMarkers();
	            do_get_draw_markers.execute("onRequest");
	            do_get_draw_markers = null;
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
		// First make sure the user really wants to report a tornado
		new AlertDialog.Builder(this)
        	.setIcon(android.R.drawable.ic_dialog_alert)
        	.setTitle("Really??")
        	.setMessage("Are you sure you want to report a tornado?")
        	.setPositiveButton("Yes", new DialogInterface.OnClickListener()
        {
        	public void onClick(DialogInterface dialog, int which) {
        		reallyPlaceMarker();
        	}

        })
        	.setNegativeButton("No", null)
        	.show();
	}
	
	private void reallyPlaceMarker() {
		mapOverlays = mapView.getOverlays();
		drawableLow = this.getResources().getDrawable(R.drawable.locationplace);
		itemizedOverlayLow = new TornadoItemizedOverlay(drawableLow);
        GeoPoint point = myLocationOverlay.getMyLocation();
        if (point != null) {
        	itemizedOverlayLow.addOverlay(new OverlayItem(point, "", ""));
        	mapOverlays.add(itemizedOverlayLow);
        	mapView.postInvalidate();
        	AsyncSubmitCoords submit_coords = new AsyncSubmitCoords();
   			submit_coords.execute(point.getLongitudeE6(), point.getLatitudeE6());
   			
   			// Show a popup, letting the user know the marker has been submitted.
   			new AlertDialog.Builder(this)
   				.setIcon(android.R.drawable.ic_dialog_alert)
   				.setTitle("Tornado submitted")
        		.setMessage("Thank you. Your tornado will be sent out a soon as we confirm it!")
        		.setPositiveButton("OK", new DialogInterface.OnClickListener()
        	{
        		public void onClick(DialogInterface dialog, int which) {
       			}
        	}).show();
        } else {
        	Toast.makeText(this, "Waiting for GPS location...", Toast.LENGTH_SHORT).show();
        }
	}
	
	class AsyncSubmitCoords extends AsyncTask<Integer, Void, Void> {
		String screenmsg = null;
		URL url;
		protected Void doInBackground(Integer... param) {
			try {
				String queryString = "?lng=" + param[0] + "&lat=" + param[1] + "&registrationId=" + showRegistrationId();
				url = new URL(CGI_BASE + "/user_submit.py" + queryString);
			} catch (MalformedURLException e) {
				e.printStackTrace();
			}
			try {
				URLConnection urlConnection = url.openConnection();
				InputStream in = new BufferedInputStream(urlConnection.getInputStream());
				InputStreamReader is = new InputStreamReader(in);
				BufferedReader br = new BufferedReader(is);
				String read = br.readLine();
				if (read != null) {
					Log.d(TAG, read);
				}
				screenmsg = read;
			} catch (IOException e) {
				screenmsg = "Unable to submit marker - try again!";
			}
			return null;
		}
		
		protected void onPostExecute(String msg) {
			Toast.makeText(TornadoWatchActivity.this, msg, Toast.LENGTH_SHORT).show();
		}
	}
	
//	public void submitCoordinates(int lng, int lat) throws MalformedURLException {
//		String queryString = "?lng=" + lng + "&lat=" + lat + "&registrationId=" + showRegistrationId();
//		URL url = new URL(CGI_BASE + "/user_submit.py" + queryString);
//		try {
//			URLConnection urlConnection = url.openConnection();
//			InputStream in = new BufferedInputStream(urlConnection.getInputStream());
//			InputStreamReader is = new InputStreamReader(in);
//			BufferedReader br = new BufferedReader(is);
//			String read = br.readLine();
//			Log.d(TAG, read);
//			Toast.makeText(this, read, Toast.LENGTH_SHORT).show();
//		} catch (IOException e) {
//			Toast.makeText(this, "Unable to submit URL - try again!", Toast.LENGTH_SHORT).show();
//		}
//	}
	
	public static String downloadJSON() throws MalformedURLException, IOException {
        Log.i(TAG, "Downloading markers");
		String url = CGI_BASE + "/get_markers.py";
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
		String registrationId = prefs.getString(AUTH, "None");
		Log.d("C2DM RegId requested", registrationId);
		return registrationId;
	}
}