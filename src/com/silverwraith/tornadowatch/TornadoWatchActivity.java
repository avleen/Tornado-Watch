package com.silverwraith.tornadowatch;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.graphics.drawable.Drawable;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapView;
import com.google.android.maps.MyLocationOverlay;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;
import com.google.android.maps.Projection;

import android.view.GestureDetector.OnDoubleTapListener;
import android.view.GestureDetector.OnGestureListener;
import android.view.MotionEvent;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

public class TornadoWatchActivity extends MapActivity implements OnGestureListener, OnDoubleTapListener {
	
	MapView mapView;
	List<Overlay> mapOverlays;
	Drawable drawable;
	TornadoItemizedOverlay itemizedOverlay;
    MyLocationOverlay myLocationOverlay;

	/** Called when the activity is first created. */
    @Override
    /** public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main); */
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    	setContentView(R.layout.main);

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
			drawable = this.getResources().getDrawable(R.drawable.androidmarker);
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

        // Jump to the users location if possible
        // TODO(avleen): Add try / catch in case we can't.
        zoomToMyLocation();
    }
    
    @Override
    protected boolean isRouteDisplayed() {
    	return false;
    }

    protected void onResume() {
    	super.onResume();
    	// On start or resume, register for location updates!
    	myLocationOverlay.enableCompass();
    	myLocationOverlay.enableMyLocation();
    	myLocationOverlay.runOnFirstFix(new Runnable() {
            public void run() {
                mapView.getController().animateTo(myLocationOverlay.getMyLocation());
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
		/* Only do this while we debug, this needs to move to a
		 * dedicated button, using Google's Android stencils maybe?
		 */
		// zoomToMyLocation();
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
	
	private void zoomToMyLocation() {
		GeoPoint myLocationGeoPoint = myLocationOverlay.getMyLocation();
		if (myLocationGeoPoint != null) {
			mapView.getController().animateTo(myLocationGeoPoint);
			mapView.getController().setZoom(10);
		} else {
			Toast.makeText(this, "Cannot determine location", Toast.LENGTH_SHORT).show();
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
}