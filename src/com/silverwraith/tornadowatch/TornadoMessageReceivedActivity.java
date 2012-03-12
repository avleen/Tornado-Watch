package com.silverwraith.tornadowatch;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

public class TornadoMessageReceivedActivity extends Activity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		setContentView(R.layout.activity_result);
		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			String message = extras.getString("payload");
			if (message != null && message.length() > 0) {
				TextView view = (TextView) findViewById(R.id.result);
				view.setText(message);
			}
		}

		super.onCreate(savedInstanceState);
	}

}