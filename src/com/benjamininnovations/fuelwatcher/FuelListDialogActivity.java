package com.benjamininnovations.fuelwatcher;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;

import com.benjamininnovations.fuelwatcher.DisplayPrices.SectionsPagerAdapter;

public class FuelListDialogActivity extends Activity implements OnClickListener {

	public static final String EXTRA_TITLE = "com.benjaminnovations.fuelwatcher.fuellistdialog.title";
	public static final String EXTRA_IS_FAVOURITE = "com.benjaminnovations.fuelwatcher.fuellistdialog.isfavvers";
	public static final String EXTRA_TRADING_NAME = "com.benjaminnovations.fuelwatcher.fuellistdialog.trading_name";

	private static final String TAG = "FuelListDialog";

	private CheckBox mFavCheckBox;
	private CheckBox mMapCheckBox;

	private static String mTradingName;
	private static FavDatabase mFavDatabase;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.fuel_list_dialog);

		Intent intent = getIntent();
		String title = intent.getStringExtra(EXTRA_TITLE);
		String trading_name = intent.getStringExtra(EXTRA_TRADING_NAME);
		mFavCheckBox = (CheckBox) findViewById(R.id.checkbox_favourite);
		mMapCheckBox = (CheckBox) findViewById(R.id.checkbox_map);
		mTradingName = intent.getStringExtra(EXTRA_TRADING_NAME);
		mFavDatabase = new FavDatabase(this);

		String msg;
		boolean isFavourite = mFavDatabase.isFavourite(trading_name);
		if (isFavourite) {
			msg = "Was a favourite";
		} else {
			msg = "Was NOT a favourite";
		}
		Log.i(TAG, msg);

		mFavCheckBox.setChecked(isFavourite);
		mMapCheckBox.setChecked(false);
		mFavCheckBox.setOnClickListener(this);
		mMapCheckBox.setOnClickListener(this);
		setTitle(title);
	}

	@Override
	public void onClick(View view) {

		if ((CheckBox) view == mMapCheckBox) {
			Log.i(TAG, "Map box clicked");
			new Thread() {
				public void run() {
					SectionsPagerAdapter.updateCameraPosition();
				}
			}.start();
			finish();
		} else if ((CheckBox) view == mFavCheckBox) {
			Log.i(TAG, "Favourite box clicked");

			new Thread() {
				public void run() {
					mFavDatabase.setFavourite(mTradingName,
							mFavCheckBox.isChecked());
				}
			}.start();
		}
	}
}