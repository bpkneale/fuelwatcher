package com.benjamininnovations.fuelwatcher;

import android.app.Application;
import android.database.Cursor;
import android.location.Location;

import com.google.android.gms.maps.model.MarkerOptions;

public class MainApplication extends Application {
	
	private static FuelDatabase fueldb;
	private static Cursor cursor;
	private static MarkerOptions[] mMarkerOptionsArray;
	private static float[] mHueArray;
	private static float maxPrice;
	private static float minPrice;
	public Location mLocation;

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

	public static float getMinPrice() {
		return minPrice;
	}

	public void setMinPrice(float minPrice) {
		MainApplication.minPrice = minPrice;
	}

	public float getMaxPrice() {
		return maxPrice;
	}

	public void setMaxPrice(float maxPrice) {
		MainApplication.maxPrice = maxPrice;
	}

	public float[] getHueArray() {
		return mHueArray;
	}

	public void setHueArray(float[] hueArray) {
		mHueArray = hueArray;
	}
}
