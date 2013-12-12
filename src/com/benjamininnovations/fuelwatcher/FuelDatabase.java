package com.benjamininnovations.fuelwatcher;
import java.util.GregorianCalendar;
import java.util.SimpleTimeZone;
import java.util.TimeZone;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class FuelDatabase extends SQLiteOpenHelper {
		
	private static int VERSION = 12;
	private static final String TAG = "FuelDatabase";
	
	public static final int DAY_TODAY = 0;
	public static final int DAY_TOMORROW = 1;
	
	private SimpleTimeZone aest;
	
	private static String[] columns;

	FuelDatabase(Context context) {
        super(context, "data", null, VERSION);
        columns = null;
        
        String[] ids = TimeZone.getAvailableIDs(8 * 60 * 60 * 1000);
        aest = new SimpleTimeZone(8 * 60 * 60 * 1000, ids[0]);
    }
	
	public void initDatabase(String[] col) {
    	columns = col;
        
        SQLiteDatabase db = getWritableDatabase();
        
        if(db != null)
        {
        	createTableFromColumns(db, columns);
        }
	}

    @Override
    public void onCreate(SQLiteDatabase db) {
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
                + newVersion + ", which will destroy all old data");
        db.execSQL("DROP TABLE IF EXISTS fuel");
    }
    
    public boolean hasTodaysValues() {
    	if (!isTableExists()) {
    		return false;
    	}
    	long time = getTodaysTimestamp();
    	Cursor cur = getCursorFromQuery(String.format("SELECT COUNT(*) FROM (SELECT _id FROM fuel WHERE _date >= %d)", time));
    	int count = cur.getInt(0);
    	cur.close();
    	return count > 0;
    }
    
    public boolean hasTomorrowsPirces() {
    	if (!hasTodaysValues()) {
    		return false;
    	}
    	return !getStringValueFromQuery("SELECT MAX(tmr_price) FROM fuel").equals("0");
    }
    
    public boolean isTableExists()
    {
        Cursor cursor = getCursorFromQuery("SELECT COUNT(*) FROM sqlite_master WHERE type = 'table' AND name = 'fuel'");
        int count = cursor.getInt(0);
        cursor.close();
        return count > 0;
    }
    
    public long getTodaysTimestamp() {
    	long dayAsMillis = 1000*60*60*24;
    	long time = new GregorianCalendar(aest).getTimeInMillis();
    	time = (time + (dayAsMillis / 2)) / dayAsMillis;
    	return time;
    }
    
    public void dropOld() {
    	Log.i(TAG, "Dropping old fuel data");
    	if(isTableExists()) {
	    	long time = getTodaysTimestamp();
	    	getWritableDatabase().execSQL(String.format("DELETE FROM fuel WHERE _date < %d", time));
    	}
    }
    
    public void dropAll() {
    	Log.i(TAG, "Dropping fuel database table");
    	getWritableDatabase().execSQL("DROP TABLE IF EXISTS fuel");
    }
    
    public double getMinimumPrice() {
    	return getMinimumPrice(DAY_TODAY);
    }
    
    public double getMinimumPrice(int day) {
    	switch(day) { 
		default:
    	case DAY_TODAY:
    		return getDoubleValueFromQuery("SELECT MIN(price) FROM fuel");
    	
    	case DAY_TOMORROW:
    		return getDoubleValueFromQuery("SELECT MIN(tmr_price) FROM fuel");
    	}
    }
    
    public double getMaximumPrice() {
    	return getMaximumPrice(DAY_TODAY);
    }
    
    public double getMaximumPrice(int day) {
    	switch(day) { 
		default:
    	case DAY_TODAY:
    		return getDoubleValueFromQuery("SELECT MAX(price) FROM fuel");
    	
    	case DAY_TOMORROW:
    		return getDoubleValueFromQuery("SELECT MAX(tmr_price) FROM fuel");
    	}
    }
    
    public double getAveragePrice() {
    	return getAveragePrice(DAY_TODAY);
    }
    
    public double getAveragePrice(int day) {
    	switch(day) { 
		default:
    	case DAY_TODAY:
    		return getDoubleValueFromQuery("SELECT AVG(price) FROM fuel");
    	
    	case DAY_TOMORROW:
    		return getDoubleValueFromQuery("SELECT AVG(tmr_price) FROM fuel");
    	}
    }
    
    public Cursor getCursorFromQuery(String query) {
    	SQLiteDatabase db = getReadableDatabase();
    	Cursor cur = db.rawQuery(query, null);
    	cur.moveToFirst();
    	return cur;
    }
    
	private void createTableFromColumns(SQLiteDatabase db, String[] columns) {
    	String dbCreate = "CREATE TABLE IF NOT EXISTS fuel (_id INTEGER PRIMARY KEY AUTOINCREMENT," +
    			"_date INTEGER,";
    	for(int i = 0; i < columns.length; i++)
    	{
    		if(i + 1  < columns.length)
    		{
        		dbCreate += String.format("%s NOT NULL,", columns[i]);	
    		}
    		else
    		{
    			dbCreate += String.format("%s NOT NULL)", columns[i]);
    		}
    	}
    	Log.i(TAG, String.format("Creating table from SQL: %s", dbCreate));
        db.execSQL(dbCreate);
    }
    
    private double getDoubleValueFromQuery(String query) {
    	return getCursorFromQuery(query).getDouble(0);
    }
    
    private String getStringValueFromQuery(String query) {
    	return getCursorFromQuery(query).getString(0);
    }
}