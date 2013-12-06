package com.benjamininnovations.fuelwatcher;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class FavDatabase extends SQLiteOpenHelper {

	private static int VERSION = 10;
	private static final String TAG = "FavDatabase";
	
	FavDatabase(Context context) {
		super(context, "favvers", null, VERSION);
//		dump();
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL("CREATE TABLE fav (_id INTEGER PRIMARY KEY AUTOINCREMENT," +
									 "trading_name TEXT NOT NULL)");
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		db.execSQL("DROP TABLE IF EXISTS fav");
		onCreate(db);
	}
	
	public String[] getListOfFavourites() {
		Cursor cur = getReadableDatabase().rawQuery("SELECT trading_name FROM fav", null);
		cur.moveToFirst();
		int count = cur.getCount();
		String[] favs = new String[count];
		for(int i = 0; i < count; i++) {
			favs[i] = cur.getString(0);
			cur.moveToNext();
		}
		return favs;
	}
	
	public void setFavourite(String trading_name, boolean fav) {
		trading_name = tradingNameConversion(trading_name);
		Log.i(TAG, String.format("Adding %s as a favourite", trading_name));
		SQLiteDatabase db = getWritableDatabase();
		db.beginTransaction();
		String selectArgs[] = new String[1];
		if(fav) {
			Log.i(TAG, String.format("Adding %s as a favourite", trading_name));
			selectArgs[0] = trading_name;
			db.execSQL("INSERT INTO fav (trading_name) VALUES (?)", selectArgs);
		}
		else {
			Log.i(TAG, String.format("Removing %s from favourites", trading_name));
			selectArgs[0] = trading_name;
			db.delete("fav", "trading_name = ?", selectArgs);
		}
		db.setTransactionSuccessful();
		db.endTransaction();
	}

	public boolean isFavourite(String trading_name) {
		trading_name = tradingNameConversion(trading_name);
		Log.i(TAG, String.format("Checking for favourite %s", trading_name));
		String selectArgs[] = new String[1];
		selectArgs[0] = trading_name;
		Cursor cur = getReadableDatabase().rawQuery("SELECT trading_name FROM fav WHERE trading_name = ?", selectArgs);
		return cur.getCount() > 0;
	}
	
	private String tradingNameConversion(String trading_name) {
		return trading_name.replace(" ", "_");
	}
	
	private void dump() {
		Cursor cur = getReadableDatabase().rawQuery("SELECT trading_name from fav", null);
		cur.moveToFirst();
		for(int i = 0; i < cur.getCount(); i++) {
			Log.i(TAG, cur.getString(0));
			cur.moveToNext();
		}
	}
}
