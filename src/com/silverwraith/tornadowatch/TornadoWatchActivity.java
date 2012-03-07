package com.silverwraith.tornadowatch;

import java.util.List;

import android.graphics.drawable.Drawable;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;
import com.google.android.maps.Projection;

import android.view.GestureDetector.OnDoubleTapListener;
import android.view.GestureDetector.OnGestureListener;
import android.view.MotionEvent;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;

public class TornadoWatchActivity extends MapActivity implements OnGestureListener, OnDoubleTapListener {
	
	MapView mapView;
	List<Overlay> mapOverlays;
	Drawable drawable;
	TornadoItemizedOverlay itemizedOverlay;

	/** Called when the activity is first created. */
    @Override
    /** public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main); */
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    	setContentView(R.layout.main);

    	mapView = (MapView) findViewById(R.id.mapView);
        LinearLayout zoomLayout = (LinearLayout)findViewById(R.id.zoom);  
        mapView.setBuiltInZoomControls(true);
        mapView.displayZoomControls(true);
        
        mapOverlays = mapView.getOverlays();
        drawable = this.getResources().getDrawable(R.drawable.androidmarker);
        itemizedOverlay = new TornadoItemizedOverlay(drawable);
        
        GeoPoint point = new GeoPoint(19240000,-99120000);
        OverlayItem overlayitem = new OverlayItem(point, "", "");
        itemizedOverlay.addOverlay(overlayitem);
        mapOverlays.add(itemizedOverlay);
    }
    
    @Override
    protected boolean isRouteDisplayed() {
    	return false;
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
		// TODO Auto-generated method stub
		return false;
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
}