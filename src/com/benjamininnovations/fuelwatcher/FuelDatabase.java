package com.benjamininnovations.fuelwatcher;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class FuelDatabase extends SQLiteOpenHelper {
		
	private static int VERSION = 1;
	private static final String TAG = "Yeah";
	
	private static String[] columns;

	FuelDatabase(Context context, String[] col) {
        super(context, "data", null, VERSION);
		columns = col;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
    	String dbCreate = "create table if not exists fuel (_id integer primary key autoincrement,";
    	for(int i = 0; i < columns.length; i++)
    	{
    		if(i + 1  < columns.length)
    		{
        		dbCreate += String.format("%s not null,", columns[i]);	
    		}
    		else
    		{
    			dbCreate += String.format("%s not null)", columns[i]);
    		}
    	}
        db.execSQL(dbCreate);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
                + newVersion + ", which will destroy all old data");
        db.execSQL("DROP TABLE IF EXISTS fuel");
        onCreate(db);
    }
    
    public String[] getCheapest(int num) {
    	String[] cheaps = new String[num];
    	Cursor cur = getCursorFromQuery(String.format("SELECT trading_name, phone, price FROM fuel ORDER BY price ASC LIMIT %d", num));
    	for(int i = 0; i < cur.getColumnCount(); i++) {
    		cheaps[i] = cur.getString(i);
    	}    	
    	return cheaps;
    }
    
    public String getMinimumPrice() {
    	return getStringValueFromQuery("SELECT MIN(price) FROM fuel");
    }
    
    public String getMaximumPrice() {
    	return getStringValueFromQuery("SELECT MAX(price) FROM fuel");
    }
    
    public String getAveragePrice() {
    	return getStringValueFromQuery("SELECT AVG(price) FROM fuel");
    }
    
    private Cursor getCursorFromQuery(String query) {
    	SQLiteDatabase db = getReadableDatabase();
    	Cursor cur = db.rawQuery(query, null);
    	cur.moveToFirst();
    	return cur;
    }
    
    private String getStringValueFromQuery(String query) {
    	return getCursorFromQuery(query).getString(0);
    }
}