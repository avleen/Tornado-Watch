package com.silverwraith.tornadowatch;

import android.content.Context;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.GestureDetector.OnDoubleTapListener;
import android.view.GestureDetector.OnGestureListener;
import android.view.MotionEvent;

import com.google.android.maps.MapView;

public class TornadoMapView extends MapView {

	private GestureDetector gestureDetector;
	
	public TornadoMapView(Context context, AttributeSet attrs) {
		super(context, attrs);
		
		gestureDetector = new GestureDetector((OnGestureListener) context);
		gestureDetector.setOnDoubleTapListener((OnDoubleTapListener) context);
	}
	
	public boolean onTouchEvent(MotionEvent ev) {
		if (this.gestureDetector.onTouchEvent(ev)) {
			return true;
		} else {
			return super.onTouchEvent(ev);
		}
	}
	
}
