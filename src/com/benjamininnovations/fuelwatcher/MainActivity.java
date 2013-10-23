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
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

public class MainActivity extends Activity {
		
	public static InputStream rssStream;
	public static ProgressBar prog;
	public static TextView loadingText;
	public static TextView resultsText;
	public static TextView fetch;
	public static NodeList nodes;
	
	public static double avgPrice;
	public static double minPrice;
	public static double maxPrice;
	
	public static int Index;
	public static int Length;

	public static FuelDatabase fueldb;
	public static MainApplication mainApp;
	
	public static LocationManager mLocationManager;
	private static Location mRoughLocation;
	
	public final static String EXTRA_MESSAGE = "com.benjamininnovations.fuelwatcher.ViewPrices";
	
	private static SQLiteDatabase db;

	private static final String FUELWATCH_RSS = "http://www.fuelwatch.wa.gov.au/fuelwatch/fuelWatchRSS?";
	
	public static MarkerOptions[] mMarkerOptionsArray;

	public static final double MaxLongitude = 100;
	public static final double MaxLatitude = 100;
	public static final double NEAR_ME_BOUNDS = 0.04;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        mainApp = (MainApplication) getApplication();
        fueldb = mainApp.getDatabase();
        prog = (ProgressBar) findViewById(R.id.progressBar1);
        loadingText = (TextView) findViewById(R.id.fetchingData);
        resultsText = (TextView) findViewById(R.id.results);
        
        mainApp.mLocation = mRoughLocation;
        
        mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        mRoughLocation = mLocationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);

        if(fueldb == null)
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
        	new Thread(new Runnable() {
        		public void run() {
        			showFuelPrices();
        		}
        	}).start();
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
    
    public void showPrices(View view) {
    	new Thread(new Runnable() {
    		public void run() {
    			showPricesFromQuery("SELECT _id, title, latitude, longitude FROM fuel ORDER BY price ASC");
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
    
    private void doNearMe() {
    	double lat = mRoughLocation.getLatitude();
    	double lng = mRoughLocation.getLongitude();
    	
    	showPricesFromQuery(String.format("SELECT _id, title, latitude, longitude FROM fuel"
    			+ " WHERE (latitude < %f AND latitude > %f AND longitude < %f AND longitude > %f)" +
    			" ORDER BY price ASC", lat + NEAR_ME_BOUNDS, lat - NEAR_ME_BOUNDS,
    			lng + NEAR_ME_BOUNDS, lng - NEAR_ME_BOUNDS));
    }
    
    private void showPricesFromQuery(String query) {

    	Cursor cur = fueldb.getCursorFromQuery(query);
    	
    	int count = cur.getCount();
        
        mMarkerOptionsArray = new MarkerOptions[count];
    	
    	for(int i = 0; i < count; i++)
    	{
    		String title = cur.getString(1);
    		LatLng latlng = new LatLng(cur.getDouble(2), cur.getDouble(3));
    		
    		mMarkerOptionsArray[i] = new MarkerOptions();
    		
    		mMarkerOptionsArray[i].title(title);
    		mMarkerOptionsArray[i].position(latlng);
    		
    		cur.moveToNext();
    	}
    	
    	cur.close();
    	
    	cur = fueldb.getCursorFromQuery(query);

    	MainApplication app = (MainApplication)getApplication();
    	
    	app.setCursor(cur);
    	app.setMarkerOptionsArray(mMarkerOptionsArray);
    	
    	Intent intent = new Intent(this, DisplayPrices.class);
    	startActivity(intent);
    }
    
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private void downloadAndParseRss() {
    	
    	try {
    		// Parse the XML into a document, and then insert the item nodes
    		// into a SQL database
	    	
	    	DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
	    	DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
	    	
	    	Document doc = dBuilder.parse(FUELWATCH_RSS);
	    	doc.normalizeDocument();
	    	
	    	nodes = doc.getElementsByTagName("item");
			
			fetch = (TextView) findViewById(R.id.fetchingData);
			fetch.post(new Runnable() {
				public void run() {
					fetch.setText(String.format("Parsing Sites"));
				}
			});
	    	
			Length = nodes.getLength();
	    	for(Index = 0; Index < Length; Index++)
	    	{
		    	ContentValues servo = new ContentValues();
	    		
		    	NodeList servonodes = nodes.item(Index).getChildNodes();
		    	String[] columns = new String[servonodes.getLength()];
		    	
		    	for(int j = 0; j < servonodes.getLength(); j++)
		    	{
		    		Node item = servonodes.item(j);
		    		String cleanedName =  item.getNodeName().replace("-", "_");
		    		String content = item.getTextContent();
		    		columns[j] = cleanedName;
		    		servo.put(cleanedName, content);
		    	}
		    	
		    	if(fueldb == null)
		    	{
		    		fueldb = new FuelDatabase(this, columns);
		    		
		    		fueldb.dropAll();
		    		
					db = fueldb.getWritableDatabase();
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
	    	
	    	showFuelPrices();
	    	
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
    
    private void showFuelPrices() {

    	avgPrice = fueldb.getAveragePrice();
    	minPrice = fueldb.getMinimumPrice();
    	maxPrice = fueldb.getMaximumPrice();
    	
    	// Hide the progress bar and loading text and show some info
    	resultsText.post(new Runnable() {
    		public void run() {
    			TextView tit = (TextView) findViewById(R.id.titleText);
    			Button but = (Button) findViewById(R.id.buttonShowPrices);
    			Button buto = (Button) findViewById(R.id.buttonNearMe);
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
    		}
    	});
    }
    
}
