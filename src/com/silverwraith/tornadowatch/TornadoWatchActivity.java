package com.silverwraith.tornadowatch;

import java.util.List;

import android.graphics.drawable.Drawable;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;

public class TornadoWatchActivity extends MapActivity {
	
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
}