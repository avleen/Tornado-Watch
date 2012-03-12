package com.silverwraith.tornadowatch;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

public class TornadoRegistrationResultActivity extends Activity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		setContentView(R.layout.activity_result);
		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			String registrationId = extras.getString("registration_id");
			if (registrationId != null && registrationId.length() > 0) {
				TextView view = (TextView) findViewById(R.id.result);
				view.setText(registrationId);
			}
		}

		super.onCreate(savedInstanceState);
	}
}