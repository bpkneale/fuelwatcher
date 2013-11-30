package com.benjamininnovations.fuelwatcher;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class FavDatabase extends SQLiteOpenHelper {

	private static int VERSION = 7;
	private static final String TAG = "FavDatabase";
	
	FavDatabase(Context context) {
		super(context, "favvers", null, VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL("CREATE TABLE fav (_id INTEGER PRIMARY KEY AUTOINCREMENT," +
									 "trading_name STRING NOT NULL)");
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		db.execSQL("DROP TABLE IF EXISTS fav");
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
		String selectArgs[] = new String[1];
		if(fav) {
			selectArgs[0] = "1";
			getWritableDatabase().execSQL("INSERT INTO fav (trading_name) VALUES (?)", selectArgs);
		}
		else {
			selectArgs[0] = trading_name;
			getWritableDatabase().rawQuery("DELETE FROM fav WHERE trading_name = ?", selectArgs);
		}
	}

	public boolean isFavourite(String trading_name) {
		String selectArgs[] = new String[1];
		selectArgs[0] = trading_name;
		Cursor cur = getReadableDatabase().rawQuery("SELECT _id FROM fav WHERE trading_name = ?", selectArgs);
		return cur.getCount() > 0;
	}
}
