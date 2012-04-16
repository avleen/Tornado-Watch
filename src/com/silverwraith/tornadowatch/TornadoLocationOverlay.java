/* Code samples from:
 * http://code.google.com/p/android-my-map-location
 */
package com.silverwraith.tornadowatch;

import android.content.Context;

import android.graphics.Canvas;
import android.location.Location;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;
import com.google.android.maps.MyLocationOverlay;


public class TornadoLocationOverlay extends MyLocationOverlay {
	
	public TornadoLocationOverlay(Context context, MapView mapView) {
		super(context, mapView);
	}
	
	@Override
	protected void drawMyLocation(Canvas canvas, MapView mapView,
		Location lastFix, GeoPoint myLocation, long when) {
			super.drawMyLocation(canvas, mapView, lastFix, myLocation, when);
	}

}
