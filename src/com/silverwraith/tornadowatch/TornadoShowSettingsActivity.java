package com.silverwraith.tornadowatch;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.TextView;

public class TornadoShowSettingsActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {

	super.onCreate(savedInstanceState);
	setContentView(R.layout.show_settings_layout);

	SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
	StringBuilder builder = new StringBuilder();
	builder.append("\n" + sharedPrefs.getBoolean("high_priority_vibrate", true));
	builder.append("\n" + sharedPrefs.getBoolean("high_priority_play_sound", true));
	builder.append("\n" + sharedPrefs.getString("high_priority_ringtone", "NULL"));
	builder.append("\n" + sharedPrefs.getBoolean("low_priority_vibrate", true));
	builder.append("\n" + sharedPrefs.getBoolean("low_priority_play_sound", true));
	builder.append("\n" + sharedPrefs.getString("low_priority_ringtone", "NULL"));

	TextView settingsTextView = (TextView) findViewById(R.id.settings_text_view);
	settingsTextView.setText(builder.toString());

	}

}