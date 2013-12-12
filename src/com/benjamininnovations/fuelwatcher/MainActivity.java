package com.benjamininnovations.fuelwatcher;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.ActionBar.OnNavigationListener;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

public class MainActivity extends Activity implements OnClickListener, OnNavigationListener {
		
	public static InputStream rssStream;
	public static ProgressBar prog;
	public static TextView loadingText;
	public static TextView resultsText;
	public static TextView fetch;
	public static NodeList nodes;
	
	public static double avgPrice;
	public static double minPrice;
	public static double maxPrice;

	public static final int QUERY_TITLE_COLUMN = 1;
	public static final int QUERY_LATITUDE_COLUMN = 2;
	public static final int QUERY_LONGITUDE_COLUMN = 3;
	public static final int QUERY_PRICE_COLUMN = 4;
	public static final int QUERY_TRADING_NAME_COLUMN = 5;
	
	public static int Index;
	public static int Length;

	public static FuelDatabase fueldb;
	public static MainApplication mainApp;
	
	private static FavDatabase mFavDatabase;
	
	public static LocationManager mLocationManager;
	private static Location mRoughLocation;
	
	public final static String EXTRA_MESSAGE = "com.benjamininnovations.fuelwatcher.ViewPrices";
	
	private static SQLiteDatabase db;

	private static final String FUELWATCH_RSS = "http://www.fuelwatch.wa.gov.au/fuelwatch/fuelWatchRSS?";
	private static final String FUELWATCH_PRODUCT_FMT = "Product=%d";
	private static final String FUELWATCH_BRAND_FMT = "&Brand=%d";
	private static final String FUELWATCH_DAY_FMT = "&Day=%d";
	private static final String[] PRODUCT_LIST = {
		"ULP",
		"PULP",
		"Diesel",
		"LPG",
		"98 RON",
		"B20 Diesel"
	};
	
	private static int mProductValue;
	private static String[] mBrands;
	
	private static final String SQL_BASE = "SELECT _id, title, latitude, longitude, price, trading_name FROM fuel ";
	private static final String TAG = "MainActivity";
	
	public static MarkerOptions[] mMarkerOptionsArray;
	public static float[] mHueArray;

	public static final double MaxLongitude = 100;
	public static final double MaxLatitude = 100;
	public static final double NEAR_ME_BOUNDS = 0.1;
	
	private static int mCheapCount;
	private static final double HUE_RED = 0.0;
	private static final double HUE_GREEN = 120.0;
	private static final double HUE_ORANGE = 20.0;
	
	private static int mNavPosition;
	private static boolean mTomorrowsPrices;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        mTomorrowsPrices = false;
        mNavPosition = 0;
        ActionBar mActionBar = getActionBar();
        mActionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
        SpinnerAdapter mSpinnerAdapter = ArrayAdapter.createFromResource(this, R.array.action_list,
                						 android.R.layout.simple_spinner_dropdown_item);
        mActionBar.setListNavigationCallbacks(mSpinnerAdapter, this);
        mActionBar.setSelectedNavigationItem(mNavPosition);
        
        mainApp = (MainApplication) getApplication();
        
        prog = (ProgressBar) findViewById(R.id.progressBar1);
        loadingText = (TextView) findViewById(R.id.fetchingData);
        resultsText = (TextView) findViewById(R.id.results);

		fueldb = new FuelDatabase(this);
		mFavDatabase = new FavDatabase(this);
		
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
				
		mProductValue = Integer.parseInt(preferences.getString(SettingsActivity.PREF_KEY_PRODUCT, "1"));
		mBrands = preferences.getStringSet(SettingsActivity.PREF_KEY_BRAND, null).toArray(new String[0]);
//		mCheapCount = Integer.parseInt(preferences.getString(SettingsActivity.PREF_KEY_NUM_CHEAP, "20"));
		mCheapCount = 20;
        
        mainApp.mLocation = mRoughLocation;
        
        mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        mRoughLocation = mLocationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);
    }
    
    @Override
    public void onResume() {
    	super.onResume();
    	
    	checkFuelUpToDate();
    }
    
    private void checkFuelUpToDate() {
        if(!fueldb.hasTodaysValues())
        {
        	showLoading(false);
        	
	        // Start lengthy operation in a background thread
	        new Thread(new Runnable() {
	            public void run() {
	                downloadAndParseRss();
	            }
	        }).start();
        }
        else
        {
        	mTomorrowsPrices = fueldb.hasTomorrowsPirces();
        	showFuelPrices(false);
        }
    }
    
    public FuelDatabase getDatabase() {
    	return fueldb;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }
    
    public boolean onOptionsItemSelected(MenuItem item) {
    	// Only have settings for now...

		Intent intent = new Intent(this, SettingsActivity.class);
		startActivity(intent);
		
    	return true;
    }

	@Override
	public void onClick(View view) {
	}
    
    public void showPrices(View view) {
    	new Thread(new Runnable() {
    		public void run() {
    			showPricesFromQuery(SQL_BASE + "WHERE " + getBrandSql() + "ORDER BY price ASC");
    		}
    	}).start();
    }
    
    public void showXCheapest(View view) {
    	new Thread(new Runnable() {
    		public void run() {
    			showPricesFromQuery(String.format(SQL_BASE + "ORDER BY price ASC LIMIT %d", mCheapCount));
    		}
    	}).start();
    }
    
    public void showNearMe(View view) {
    	new Thread(new Runnable() {
    		public void run() {
    			doNearMe();
    		}
    	}).start();
    }
    
    public void showFavvers(View view) {
    	new Thread(new Runnable() {
    		public void run() {
    			String[] favs = mFavDatabase.getListOfFavourites();
    			
    			if(favs.length > 0) {
        			String sql = SQL_BASE + "WHERE ";
        			for(String f: favs) {
        				if (f == favs[favs.length - 1]) {
        					sql += String.format("trading_name = \"%s\"", f);
        				}
        				else {
        					sql += String.format("trading_name = \"%s\" OR ", f);
        				}
        			}
        			sql += " ORDER BY price ASC";
        			
        			showPricesFromQuery(sql);
    			} else {

    				Handler handler = new Handler(Looper.getMainLooper());
    				
    				handler.post(new Runnable () {
    					public void run() {
    	    				Context context = getApplicationContext();
    	    				CharSequence text = "No favourites to show...";
    	    				int duration = Toast.LENGTH_SHORT;

    	    				Toast toast = Toast.makeText(context, text, duration);
    	    				toast.show();
    					}
    				});
    			}
    		}
    	}).start();
    }
    
    private void doNearMe() {
    	double lat = mRoughLocation.getLatitude();
    	double lng = mRoughLocation.getLongitude();
    	String sql = String.format(SQL_BASE + "WHERE latitude < %.9f AND latitude > %.9f" +
					    		   " AND longitude < %.9f AND longitude > %.9f" +
						    	   " ORDER BY price ASC",
						    	   lat + NEAR_ME_BOUNDS, lat - NEAR_ME_BOUNDS,
						    	   lng + NEAR_ME_BOUNDS, lng - NEAR_ME_BOUNDS);
    	
    	showPricesFromQuery(sql);
    }
    
    private String getBrandSql() {
    	String sql = new String();
    	for(String brandInt: mBrands) {
    		int b = Integer.parseInt(brandInt);
    		String bran = SettingsActivity.BRAND_LIST[b];
    		if (brandInt.equals(mBrands[mBrands.length - 1])) {
        		sql += String.format("brand = \"%s\" ", bran);
    		} else {
        		sql += String.format("brand = \"%s\" OR ", bran);
    		}
    	}
    	return sql;
    }
    
    private void showPricesFromQuery(String query) {
    	
    	Cursor cur = fueldb.getCursorFromQuery(query);
    	float[] prices;
    	
    	int count = cur.getCount();

    	Log.i(TAG, String.format("SQL Query: %s\nReturned %d results", query, count));
        
        mMarkerOptionsArray = new MarkerOptions[count];
        mHueArray = new float[count];
    	prices = new float[count];
    	
    	for(int i = 0; i < count; i++) {
    		String title = cur.getString(1);
    		LatLng latlng = new LatLng(cur.getDouble(QUERY_LATITUDE_COLUMN), cur.getDouble(QUERY_LONGITUDE_COLUMN));
    		prices[i] = cur.getFloat(QUERY_PRICE_COLUMN);
    		mMarkerOptionsArray[i] = new MarkerOptions();
    		mMarkerOptionsArray[i].title(title);
    		mMarkerOptionsArray[i].position(latlng);
    		cur.moveToNext();
    	}
    	
    	float avgToMin = (float)(avgPrice - minPrice);
    	
    	for(int i = 0; i < count; i++) {
    		if(maxPrice == minPrice) {
    			mHueArray[i] = (float)HUE_ORANGE;
    		}
    		else {
    			float hue = prices[i] - (float)minPrice;
    			float diff = avgToMin;
    			float ratio = (float)HUE_GREEN / diff;
    			
    			hue *= ratio;
    			hue = (hue * -1) + (float)HUE_GREEN;
    			
    			if(hue < HUE_RED) {
    				hue = (float)HUE_RED;
    			}
    			else if(hue > HUE_GREEN) {
    				hue = (float)HUE_GREEN;
    			}
    			
    			mHueArray[i] = hue;
    		}
    	}
    	
    	cur.close();
    	
    	cur = fueldb.getCursorFromQuery(query);

    	MainApplication app = (MainApplication)getApplication();
    	
    	app.setCursor(cur);
    	app.setMarkerOptionsArray(mMarkerOptionsArray);
    	app.setHueArray(mHueArray);
    	
    	Intent intent = new Intent(this, DisplayPrices.class);
    	startActivity(intent);
    }
    
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private void downloadAndParseRss() {
    	
		// Parse the XML into a document, and then insert the item nodes
		// into a SQL database
		String url = FUELWATCH_RSS;
		url += String.format(FUELWATCH_PRODUCT_FMT, mProductValue);
		
		NodeList todaysPrices = getNodesFromUrl(url);
		NodeList tommorPrices = getNodesFromUrl(url + "&Day=tomorrow");
		
		String[] tmrPrice = new String[tommorPrices.getLength()];
		if(tmrPrice.length > 0) {
			mTomorrowsPrices = true;
			Log.i(TAG, "Found data for tomorrows fuel prices!");
		} else {
			Log.i(TAG, "No data for tomorrows fuel prices...");
			mTomorrowsPrices = false;
		}
		
		if(mTomorrowsPrices) {
	    	for(int i = 0; i < tmrPrice.length; i++) {
	    		NodeList servo = tommorPrices.item(i).getChildNodes();
	    		for(int j = 0; j < servo.getLength(); j++) {
	    			Node node = servo.item(j);
	    			if (node.getNodeName().equals("price")) {
	    				tmrPrice[i] = node.getTextContent();
	    			} 
	    		}
			}
		}
		
		fueldb.dropOld();
    	
		db = fueldb.getWritableDatabase();
		boolean activeTransaction = false;
		String timestamp = String.format("%d", fueldb.getTodaysTimestamp());
    	
		nodes = getNodesFromUrl(url);
		Length = nodes.getLength();
    	for(Index = 0; Index < Length; Index++)
    	{
	    	ContentValues servo = new ContentValues();
	    	
	    	servo.put("_date", timestamp);
    		
	    	NodeList servonodes = nodes.item(Index).getChildNodes();
	    	String[] columns = new String[servonodes.getLength() + 1];
	    	columns[columns.length - 1] = "tmr_price";
	    	
	    	for(int j = 0; j < servonodes.getLength(); j++)
	    	{
	    		Node item = servonodes.item(j);
	    		String cleanedName =  item.getNodeName().replace("-", "_");
	    		String content = item.getTextContent();
	    		columns[j] = cleanedName;
	    		if (cleanedName.equals("longitude") || cleanedName.equals("latitude")) {
	    			columns[j] += " REAL";
	    		}
	    		servo.put(cleanedName, content);
	    	}
	    	
	    	if(!fueldb.isTableExists()) {
	    		fueldb.initDatabase(columns);
	    	}
	    	
	    	if(!activeTransaction) {
	    		activeTransaction = true;
				db.beginTransaction();
	    	}
	    	
	    	if(mTomorrowsPrices) {
	    		servo.put("tmr_price", tmrPrice[Index]);
	    	} else {
	    		servo.put("tmr_price", "0");
	    	}
	    	
		    db.insert("fuel", null, servo);
    	}
    	if(activeTransaction) {
			db.setTransactionSuccessful();
			db.endTransaction();
    	}	    	
    	
    	if(mainApp.getDatabase() == null)
    	{
    		mainApp.setDatabase(fueldb);
    	}
    	
    	showFuelPrices(true);
    }
    
    private NodeList getNodesFromUrl(String url) {
		Log.i(TAG, "Using URL " + url);
		
    	DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
    	DocumentBuilder dBuilder = null;
		try {
			dBuilder = dbFactory.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	
    	Document doc = null;
		try {
			doc = dBuilder.parse(url);
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	doc.normalizeDocument();
    	
    	return doc.getElementsByTagName("item");
    }
    
    private void showLoading(boolean postpone) {

    	// Show the progress bar and hide text
    	Runnable uiInteraction = new Runnable() {
    		public void run() {
    			TextView tit = (TextView) findViewById(R.id.titleText);
    			Button but = (Button) findViewById(R.id.buttonShowPrices);
    			Button buto = (Button) findViewById(R.id.buttonNearMe);
    			Button cheap = (Button) findViewById(R.id.buttonShowCheap);
    			Button favvers = (Button) findViewById(R.id.buttonFavourites);
    			tit.setText(String.format("%s prices for %s", PRODUCT_LIST[mProductValue-1], "today"));
    			tit.setVisibility(View.VISIBLE);
    			//tit.setTextAlignment(TextView.TEXT_ALIGNMENT_CENTER);
    			
	    		prog.setVisibility(View.VISIBLE);
    			loadingText.setVisibility(View.VISIBLE);
    			resultsText.setText(String.format(
    	    	"Minimum ULP price\t\t\t\t\t%.1f c/L\n\n"
    	    	+"Average ULP price\t\t\t\t\t\t%.1f c/L\n\n"
    	    	+"Maximum ULP price\t\t\t\t%.1f c/L", minPrice, avgPrice, maxPrice));
    			resultsText.setVisibility(View.INVISIBLE);
    			but.setVisibility(View.INVISIBLE);
    			buto.setVisibility(View.INVISIBLE);
    			cheap.setVisibility(View.INVISIBLE);
    			favvers.setVisibility(View.INVISIBLE);
    		}
    	};
    	
    	if(postpone) {
    		resultsText.post(uiInteraction);
    	} else {
    		uiInteraction.run();
    	}
    }
    
    private void showFuelPrices(boolean postpone) {

    	avgPrice = fueldb.getAveragePrice(mNavPosition);
    	minPrice = fueldb.getMinimumPrice(mNavPosition);
    	maxPrice = fueldb.getMaximumPrice(mNavPosition);

    	// Hide the progress bar and loading text and show some info
    	Runnable uiInteraction = new Runnable() {
    		public void run() {
    			TextView tit = (TextView) findViewById(R.id.titleText);
    			Button but = (Button) findViewById(R.id.buttonShowPrices);
    			Button buto = (Button) findViewById(R.id.buttonNearMe);
    			Button cheap = (Button) findViewById(R.id.buttonShowCheap);
    			Button favvers = (Button) findViewById(R.id.buttonFavourites);
    			tit.setText(String.format("%s prices for %s", PRODUCT_LIST[mProductValue-1], "today"));
    			tit.setVisibility(View.VISIBLE);
    			//tit.setTextAlignment(TextView.TEXT_ALIGNMENT_CENTER);
    			
	    		prog.setVisibility(View.INVISIBLE);
    			loadingText.setVisibility(View.INVISIBLE);
    			resultsText.setText(String.format(
    	    	"Minimum ULP price\t\t\t\t\t%.1f c/L\n\n"
    	    	+"Average ULP price\t\t\t\t\t\t%.1f c/L\n\n"
    	    	+"Maximum ULP price\t\t\t\t%.1f c/L", minPrice, avgPrice, maxPrice));
    			resultsText.setVisibility(View.VISIBLE);
    			but.setVisibility(View.VISIBLE);
    			buto.setVisibility(View.VISIBLE);
    			cheap.setVisibility(View.VISIBLE);
    			favvers.setVisibility(View.VISIBLE);
    		}
    	};
    	
    	if(postpone) {
    		resultsText.post(uiInteraction);
    	}
    	else {
    		uiInteraction.run();
    	}
    }

	@Override
	public boolean onNavigationItemSelected(int position, long itemId) {
		mNavPosition = position;
		switch(position) {
		default:
		case 0:
			showFuelPrices(false);
			break;
		case 1:
			if (!mTomorrowsPrices) {
				Context context = getApplicationContext();
				CharSequence text = "Tomorrows prices not available yet";
				int duration = Toast.LENGTH_SHORT;

				Toast toast = Toast.makeText(context, text, duration);
				toast.show();
				
				getActionBar().setSelectedNavigationItem(0);
			} else {
				showFuelPrices(false);
			}
			break;
		}
		Log.i(TAG, String.format("Navigation item, pos: %d, id: %d", position, itemId));
		
		return true;
	}
    
}
