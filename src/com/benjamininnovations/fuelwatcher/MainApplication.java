package com.benjamininnovations.fuelwatcher;

import android.app.Application;
import android.database.Cursor;

public class MainApplication extends Application {
	
	private static FuelDatabase fueldb;
	private static Cursor cursor;

	public MainApplication() {
		fueldb = null;
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
