package com.benjamininnovations.fuelwatcher;

import android.app.Application;
import android.database.Cursor;

import com.google.android.gms.maps.model.MarkerOptions;

public class MainApplication extends Application {
	
	private static FuelDatabase fueldb;
	private static Cursor cursor;
	private static MarkerOptions[] mMarkerOptionsArray;

	public MainApplication() {
		fueldb = null;
	}
	
	public void setMarkerOptionsArray(MarkerOptions[] arr) {
		mMarkerOptionsArray = arr;
	}
	
	public MarkerOptions[] getMarkerOptionsArray() {
		return mMarkerOptionsArray;
	}

	public void setDatabase(FuelDatabase db) {
		fueldb = db;
	}

	public FuelDatabase getDatabase() {
		return fueldb;
	}
	
	public Cursor getCursor() {
		return cursor;
	}
	
	public void setCursor(Cursor cur) {
		cursor = cur;
	}
}
