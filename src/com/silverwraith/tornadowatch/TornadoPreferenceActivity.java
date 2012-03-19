package com.silverwraith.tornadowatch;

import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.view.Menu;
import android.view.MenuItem;

public class TornadoPreferenceActivity extends PreferenceActivity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.layout.preferences);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// menu.add(Menu.NONE, 0, 0, "Show current settings");
		// return super.onCreateOptionsMenu(menu);
		return false;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case 0:
				startActivity(new Intent(this, TornadoShowSettingsActivity.class));
				return true;
		}
		return false;
	}
}
