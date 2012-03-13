/* Code samples from:
 * http://code.google.com/p/android-my-map-location
 */
package com.silverwraith.tornadowatch;

import android.content.Context;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Paint.Style;
import android.graphics.drawable.Drawable;
import android.location.Location;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;
import com.google.android.maps.MyLocationOverlay;
import com.google.android.maps.Projection;


public class TornadoLocationOverlay extends MyLocationOverlay {

	private boolean bugged = false;
	private Drawable drawable;
	private Paint accuracyPaint;
	private Point center;
	private Point left;
	private int height;
	private int width;
	
	public TornadoLocationOverlay(Context context, MapView mapView) {
		super(context, mapView);
	}
	
	@Override
	protected void drawMyLocation(Canvas canvas, MapView mapView,
		Location lastFix, GeoPoint myLocation, long when) {
			super.drawMyLocation(canvas, mapView, lastFix, myLocation, when);
	}

}