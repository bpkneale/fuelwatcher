package com.benjamininnovations.fuelwatcher;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
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
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

public class MainActivity extends Activity implements OnClickListener {
		
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
	private static int mBrandValue;
	
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        mainApp = (MainApplication) getApplication();
        
        prog = (ProgressBar) findViewById(R.id.progressBar1);
        loadingText = (TextView) findViewById(R.id.fetchingData);
        resultsText = (TextView) findViewById(R.id.results);

		fueldb = new FuelDatabase(this);
		mFavDatabase = new FavDatabase(this);
		fueldb.dropOld();
		
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
		mProductValue = Integer.parseInt(preferences.getString(SettingsActivity.PREF_KEY_PRODUCT, "1"));
		mBrandValue = Integer.parseInt(preferences.getString(SettingsActivity.PREF_KEY_BRAND, "0"));
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
	        // Start lengthy operation in a background thread
	        new Thread(new Runnable() {
	            public void run() {
	                downloadAndParseRss();
	            }
	        }).start();
        }
        else
        {
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
    			showPricesFromQuery(SQL_BASE + "ORDER BY price ASC");
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
    	
    	try {
    		// Parse the XML into a document, and then insert the item nodes
    		// into a SQL database
    		String brand = SettingsActivity.BRAND_LIST[mBrandValue];
    		String url = FUELWATCH_RSS;
    		url += String.format(FUELWATCH_PRODUCT_FMT, mProductValue);
    		if(brand != null) {
    			url += String.format(FUELWATCH_BRAND_FMT, mBrandValue);
    		}
	    	
    		Log.i(TAG, "Using URL " + url);
    		
	    	DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
	    	DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
	    	
	    	Document doc = dBuilder.parse(url);
	    	doc.normalizeDocument();
	    	
	    	nodes = doc.getElementsByTagName("item");
			
			fetch = (TextView) findViewById(R.id.fetchingData);
			fetch.post(new Runnable() {
				public void run() {
					fetch.setText(String.format("Parsing Sites"));
				}
			});
	    	
			db = fueldb.getWritableDatabase();
			boolean activeTransaction = false;
			String timestamp = String.format("%d", fueldb.getTodaysTimestamp());
	    	
			Length = nodes.getLength();
	    	for(Index = 0; Index < Length; Index++)
	    	{
		    	ContentValues servo = new ContentValues();
		    	
		    	servo.put("_date", timestamp);
	    		
		    	NodeList servonodes = nodes.item(Index).getChildNodes();
		    	String[] columns = new String[servonodes.getLength()];
		    	
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
		    	
			    db.insert("fuel", null, servo);
	    	}
			db.setTransactionSuccessful();
	    	db.endTransaction();
	    	
	    	
	    	if(mainApp.getDatabase() == null)
	    	{
	    		mainApp.setDatabase(fueldb);
	    	}
	    	
	    	showFuelPrices(true);
	    	
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
    
    private void showFuelPrices(boolean postpone) {

    	avgPrice = fueldb.getAveragePrice();
    	minPrice = fueldb.getMinimumPrice();
    	maxPrice = fueldb.getMaximumPrice();

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
    
}
