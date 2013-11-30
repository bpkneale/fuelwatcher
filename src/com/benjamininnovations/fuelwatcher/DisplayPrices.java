package com.benjamininnovations.fuelwatcher;

import android.app.ActionBar;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.NavUtils;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.SimpleCursorAdapter;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMapOptions;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
//import com.google.android.gms.maps.MapFragment;


public class DisplayPrices extends FragmentActivity implements
		ActionBar.TabListener {
	
	
	/**
	 * The {@link android.support.v4.view.PagerAdapter} that will provide
	 * fragments for each of the sections. We use a
	 * {@link android.support.v4.app.FragmentPagerAdapter} derivative, which
	 * will keep every loaded fragment in memory. If this becomes too memory
	 * intensive, it may be best to switch to a
	 * {@link android.support.v4.app.FragmentStatePagerAdapter}.
	 */
	SectionsPagerAdapter mSectionsPagerAdapter;

	/**
	 * The {@link ViewPager} that will host the section contents.
	 */
	static ViewPager mViewPager;
	
	public static DisplayPrices mDisplayPricesThis;
	
	private static GoogleMap mGoogleMap;
	private static SupportMapFragment mSupportMapFragment; 
	private static MainApplication mApplication;
	private static boolean mMapInUse;
	private static CameraPosition mNewCameraPosition;

	private static final String TAG = "DisplayPrices";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_display_prices);
		
		mDisplayPricesThis = this;
		mApplication = (MainApplication) getApplicationContext();
		mMapInUse = false;

		// Set up the action bar.
		final ActionBar actionBar = getActionBar();
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
		// Show the Up button in the action bar.
		actionBar.setDisplayHomeAsUpEnabled(true);

		// Create the adapter that will return a fragment for each of the three
		// primary sections of the app.
		mSectionsPagerAdapter = new SectionsPagerAdapter(
				getSupportFragmentManager());

		// Set up the ViewPager with the sections adapter.
		mViewPager = (ViewPager) findViewById(R.id.pager);
		mViewPager.setAdapter(mSectionsPagerAdapter);
		

		// When swiping between different sections, select the corresponding
		// tab. We can also use ActionBar.Tab#select() to do this if we have
		// a reference to the Tab.
		mViewPager
				.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
					@Override
					public void onPageSelected(int position) {
						actionBar.setSelectedNavigationItem(position);
					}
				});

		actionBar.addTab(actionBar.newTab()
				.setText("List")
				.setTabListener(this));

		actionBar.addTab(actionBar.newTab()
				.setText("Map")
				.setTabListener(this));
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.display_prices, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			// This ID represents the Home or Up button. In the case of this
			// activity, the Up button is shown. Use NavUtils to allow users
			// to navigate up one level in the application structure. For
			// more details, see the Navigation pattern on Android Design:
			//
			// http://developer.android.com/design/patterns/navigation.html#up-vs-back
			//
			NavUtils.navigateUpFromSameTask(this);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onTabSelected(ActionBar.Tab tab,
			FragmentTransaction fragmentTransaction) {
		// When the given tab is selected, switch to the corresponding page in
		// the ViewPager.
		int pos = tab.getPosition();
		
		mViewPager.setCurrentItem(pos);
	}

	@Override
	public void onTabUnselected(ActionBar.Tab tab,
			FragmentTransaction fragmentTransaction) {
	}

	@Override
	public void onTabReselected(ActionBar.Tab tab,
			FragmentTransaction fragmentTransaction) {
	}

	/**
	 * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
	 * one of the sections/tabs/pages.
	 */
	public static class SectionsPagerAdapter extends FragmentPagerAdapter {
		
		public static final int POSITION_LIST = 0;
		public static final int POSITION_MAP = 1;
		
		private final LatLng PERTH_LATLNG = new LatLng(-31.9688837, 115.9313409);
		private final GoogleMapOptions MAP_OPTIONS = new GoogleMapOptions().camera(new CameraPosition(PERTH_LATLNG, 9, 0, 0));

		public SectionsPagerAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override
		public Fragment getItem(int position) {
			Fragment fragment;
			
			switch(position){
				default:
				case POSITION_LIST:
					fragment = new FuelListSectionFragment();
					break;
					
				case POSITION_MAP:
					if(mSupportMapFragment == null) {
						fragment = SupportMapFragment.newInstance(MAP_OPTIONS);
						mSupportMapFragment = (SupportMapFragment)fragment;
					}
					else {
						fragment = mSupportMapFragment;
					}
					mGoogleMap = null;
					
			        new Thread(new Runnable() {
			            public void run() {
			            	keepMapUpdated();
			            }
			        }).start();
			        
					break;
			}
			return fragment;
		}
		
		public static void keepMapUpdated() {
			
			while(mGoogleMap == null)
			{
				SystemClock.sleep(100);
				mGoogleMap = mSupportMapFragment.getMap();
			}
			
			Handler handler = new Handler(Looper.getMainLooper());
			
			handler.post(new Runnable() {
				public void run() {
					
					MarkerOptions[] marks = mApplication.getMarkerOptionsArray();
					float[] hues = mApplication.getHueArray();
					BitmapDescriptor bmp;
					
					int arrLen = marks.length;
								
					for(int i = 0; i < arrLen; i++) {
						bmp = BitmapDescriptorFactory.defaultMarker(hues[i]);
						marks[i].icon(bmp);
						mGoogleMap.addMarker(marks[i]);
					}
				}
			});
			
		}
		
		public static void updateCameraPosition() {
			
			while(mGoogleMap == null) {
				SystemClock.sleep(100);
			}
			
			Handler handler = new Handler(Looper.getMainLooper());
			
			handler.post(new Runnable() {
				public void run() {
					mGoogleMap.moveCamera(CameraUpdateFactory.newCameraPosition(mNewCameraPosition));
					mViewPager.setCurrentItem(SectionsPagerAdapter.POSITION_MAP);
				}
			});
			
		}

		@Override
		public int getCount() {
			return 2;
		}

		@Override
		public CharSequence getPageTitle(int position) {
			return null;
		}
	}

	/**
	 * A dummy fragment representing a section of the app, but that simply
	 * displays dummy text.
	 */
	public static class FuelListSectionFragment extends Fragment {
		/**
		 * The fragment argument representing the section number for this
		 * fragment.
		 */
		
		public FuelListSectionFragment() {
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			View rootView = inflater.inflate(
					R.layout.fragment_display_prices_dummy, container, false);
			Context context = rootView.getContext();
			
			ListView fuelView = (ListView) rootView.findViewById(R.id.fuelPriceList);
			Cursor cursor = mApplication.getCursor();
			
			String[] from = new String[] {"title"};
			int[] to = new int[] {android.R.id.text1};
			
			SimpleCursorAdapter adapt = new SimpleCursorAdapter(context, android.R.layout.simple_list_item_1, cursor, from, to, 0);
			fuelView.setAdapter(adapt);
			
			MyOnClickListener listener = new MyOnClickListener();
			fuelView.setOnItemClickListener(listener);
				
			return rootView;
		}
		
		public void itemClick() {
			
		}
	}
	
	public static class MyOnClickListener implements AdapterView.OnItemClickListener {
		
		public MyOnClickListener() {
			
		}
		
		@Override
		public void onItemClick (AdapterView<?> parent, View view, int position, long id) {
			Cursor cur = ((SimpleCursorAdapter)parent.getAdapter()).getCursor();
			cur.move(position);
			
			// Setup where the map should point to, should the user select that option
			LatLng servo = new LatLng(cur.getDouble(MainActivity.QUERY_LATITUDE_COLUMN),
								      cur.getDouble(MainActivity.QUERY_LONGITUDE_COLUMN));
			String title = cur.getString(MainActivity.QUERY_TITLE_COLUMN);
			String trading = cur.getString(MainActivity.QUERY_TRADING_NAME_COLUMN);
			mNewCameraPosition = new CameraPosition(servo, 12, 0, 0);
			
	    	Intent intent = new Intent(mDisplayPricesThis, FuelListDialogActivity.class);
	    	intent.putExtra(FuelListDialogActivity.EXTRA_TITLE, title);
	    	intent.putExtra(FuelListDialogActivity.EXTRA_TRADING_NAME, trading);
	    	mDisplayPricesThis.startActivity(intent);
		}
	}
}
