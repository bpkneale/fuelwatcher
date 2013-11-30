package com.benjamininnovations.fuelwatcher;

import com.benjamininnovations.fuelwatcher.DisplayPrices.SectionsPagerAdapter;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;

public class FuelListDialogActivity extends Activity implements OnClickListener {
	
	public static final String EXTRA_TITLE = "com.benjaminnovations.fuelwatcher.fuellistdialog.title";
	public static final String EXTRA_IS_FAVOURITE = "com.benjaminnovations.fuelwatcher.fuellistdialog.isfavvers";
	public static final String EXTRA_TRADING_NAME = "com.benjaminnovations.fuelwatcher.fuellistdialog.trading_name";
	
	private CheckBox mFavCheckBox;
	private CheckBox mMapCheckBox;
	
	private static String mTradingName;
	private static boolean mIsChecked;
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
		
		boolean isFavourite = mFavDatabase.isFavourite(trading_name); 
		
		mFavCheckBox.setActivated(isFavourite);
		mMapCheckBox.setActivated(false);
		mFavCheckBox.setOnClickListener(this);
		mMapCheckBox.setOnClickListener(this);
        setTitle(title);
	}

	@Override
	public void onClick(View view) {
		
		if ((CheckBox)view == mMapCheckBox) {
			new Thread() {
				public void run() {
					SectionsPagerAdapter.updateCameraPosition();
				}
			}.run();
			finish();
		}
		else if((CheckBox)view == mFavCheckBox) {
			mIsChecked = mFavCheckBox.isChecked();
			
			new Thread() {
				public void run() {
					mFavDatabase.setFavourite(mTradingName, mIsChecked);
				}
			}.start();
		}
	}
}