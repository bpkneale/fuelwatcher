package com.benjamininnovations.fuelwatcher;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

/**
 * A {@link PreferenceActivity} that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 * <p>
 * See <a href="http://developer.android.com/design/patterns/settings.html">
 * Android Design: Settings</a> for design guidelines and the <a
 * href="http://developer.android.com/guide/topics/ui/settings.html">Settings
 * API Guide</a> for more information on developing a Settings UI.
 */
public class SettingsActivity extends PreferenceActivity implements OnSharedPreferenceChangeListener {
	
	public static final String PREF_KEY_PRODUCT = "pref_key_product";
	public static final String PREF_KEY_BRAND = "pref_key_brand";
	public static final String PREF_KEY_NUM_CHEAP = "pref_key_num_cheap";
	
	public static final String[] BRAND_LIST = {
		null,
		null,
		"Ampol",
		"Better Choice",
		"BOC",
		"BP",
		"Caltex",
		"Gull",
		"Kleenheat",
		"Kwikfuel",
		"Liberty",
		null,
		null,
		"Peak",
		"Shell",
		"Independent",
		"Wesco",
		null,
		null,
		"Caltex Woolworths",
		"Coles Express",
		"Black and White",
		null,
		"United",
		"Eagle"
	};
	
	private OnSharedPreferenceChangeListener mListener;
	private static FuelDatabase mFuelDatabase;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.pref_general);

		mFuelDatabase = new FuelDatabase(this);
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		
		mListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
			  public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
					if (key.equals(PREF_KEY_PRODUCT) || key.equals(PREF_KEY_BRAND)) {
						mFuelDatabase.dropAll();
					}
			  }
		};
		prefs.registerOnSharedPreferenceChangeListener(mListener);
	}
	
	public void onSharedPreferenceChanged(SharedPreferences preferences, String key) {
	}
	
	public void erasePreferences() {
	     SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
	     Editor editor = preferences.edit();
	     editor.clear();
	     editor.commit();
	}
}
