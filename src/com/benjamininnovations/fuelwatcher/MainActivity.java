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
import android.app.Application;
import android.content.ContentValues;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

public class MainActivity extends Activity {
		
	public static InputStream rssStream;
	public static ProgressBar prog;
	public static TextView loadingText;
	public static TextView resultsText;
	public static TextView fetch;
	public static NodeList nodes;
	
	public static String avgPrice;
	public static String minPrice;
	public static String maxPrice;
	
	public static int Index;
	public static int Length;

	public static FuelDatabase fueldb;
	public static MainApplication mainApp;
	
	public final static String EXTRA_MESSAGE = "com.benjamininnovations.fuelwatcher.ViewPrices";
	
	private static SQLiteDatabase db;

	private static final String FUELWATCH_RSS = "http://www.fuelwatch.wa.gov.au/fuelwatch/fuelWatchRSS?";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        mainApp = (MainApplication) getApplication();
        fueldb = mainApp.getDatabase();

        if(fueldb == null)
        {
	        prog = (ProgressBar) findViewById(R.id.progressBar1);
	        loadingText = (TextView) findViewById(R.id.fetchingData);
	        resultsText = (TextView) findViewById(R.id.results);
	
	        // Start lengthy operation in a background thread
	        new Thread(new Runnable() {
	            public void run() {
	                downloadAndParseRss();
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
    	Intent intent = new Intent(this, DisplayPrices.class);
//    	intent.putExtra(EXTRA_MESSAGE, fueldb);
    	startActivity(intent);
    }
    
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private void downloadAndParseRss() {
    	
    	try {
    		
    		// Parse the XML into a document, and then insert the item nodes
    		// into a SQL database
    		
//			URL rss = new URL(FUELWATCH_RSS);
//			
//	    	URLConnection con = rss.openConnection();
//	    	
//	    	rssStream = con.getInputStream();
//	    	byte[] buf = new byte[4096];
//	    	rssStream.read(buf, 0, buf.length);
//	    	String rssString = new String(buf); 
	    	
	    	DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
	    	DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
	    	
	    	Document doc = dBuilder.parse(FUELWATCH_RSS);
	    	doc.normalizeDocument();
	    	
	    	nodes = doc.getElementsByTagName("item");
			fueldb = null;
			
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
	    	
	    	avgPrice = fueldb.getAveragePrice();
	    	minPrice = fueldb.getMinimumPrice();
	    	maxPrice = fueldb.getMaximumPrice();
	    	
	    	// Hide the progress bar and loading text and show some info
	    	resultsText.post(new Runnable() {
	    		public void run() {
	    			TextView tit = (TextView) findViewById(R.id.titleText);
	    			Button but = (Button) findViewById(R.id.buttonShowPrices);
	    			tit.setVisibility(View.VISIBLE);
	    			
		    		prog.setVisibility(View.INVISIBLE);
	    			loadingText.setVisibility(View.INVISIBLE);
	    			resultsText.setText(String.format("Average ULP price:\t\t\t\t%s\tc/L\n"
	    	    	+"Minimum ULP price:\t\t\t%s\tc/L\n"
	    	    	+"Maximum ULP price:\t\t\t%s\tc/L", avgPrice, minPrice, maxPrice));
	    			resultsText.setVisibility(View.VISIBLE);
	    			but.setVisibility(View.VISIBLE);
	    		}
	    	});
	    	
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
    
}
