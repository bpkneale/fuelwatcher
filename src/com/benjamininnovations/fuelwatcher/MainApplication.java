package com.benjamininnovations.fuelwatcher;

import android.app.Application;

public class MainApplication extends Application {
	
	private static FuelDatabase fueldb;

	public MainApplication() {
		fueldb = null;
	}

	public void setDatabase(FuelDatabase db) {
		fueldb = db;
	}

	public FuelDatabase getDatabase() {
		return fueldb;
	}
}
