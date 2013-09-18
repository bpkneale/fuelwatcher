package com.benjamininnovations.fuelwatcher;

import android.app.ActionBar;
import android.app.FragmentTransaction;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
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
import android.widget.ListView;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.SupportMapFragment;


public class DisplayPrices extends FragmentActivity implements
		ActionBar.TabListener {
	
	public FuelDatabase fueldb;
	public SQLiteDatabase readable;
	
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
	ViewPager mViewPager;
	
	private MapFragment mMapFragment;
	private GoogleMap map;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_display_prices);
		
		MainApplication mainApp = (MainApplication) getApplication();
		fueldb = mainApp.getDatabase();
		readable = fueldb.getReadableDatabase();
		
		map = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map)).getMap();

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
		mViewPager.setCurrentItem(tab.getPosition());
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
	public class SectionsPagerAdapter extends FragmentPagerAdapter {

		public SectionsPagerAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override
		public Fragment getItem(int position) {
			// getItem is called to instantiate the fragment for the given page.
			// Return a DummySectionFragment (defined as a static inner class
			// below) with the page number as its lone argument.
			Fragment fragment;
			
			switch(position){
			default:
			case 0:
				fragment = new FuelListSectionFragment();
				Bundle args = new Bundle();
				args.putInt(FuelListSectionFragment.ARG_SECTION_NUMBER, position + 1);
				fragment.setArguments(args);
				break;
				
			case 1:
				fragment = new FuelMapSectionFragment();
				break;
			
			}
			return fragment;
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
		public static final String ARG_SECTION_NUMBER = "section_number";

		public FuelListSectionFragment() {
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			View rootView = inflater.inflate(
					R.layout.fragment_display_prices_dummy, container, false);
			Context context = rootView.getContext();
			
			ListView fuelView = (ListView) rootView.findViewById(R.id.fuelPriceList);
			MainApplication app = (MainApplication) context.getApplicationContext();
			Cursor cur = app.getCursor();
			
			String[] from = new String[] {"title"};
			int[] to = new int[] {android.R.id.text1};
			
			SimpleCursorAdapter adapt = new SimpleCursorAdapter(context, android.R.layout.simple_list_item_1, cur, from, to, 0);
			
			fuelView.setAdapter(adapt);
				
			return rootView;
		}
	}
	
	public static class FuelMapSectionFragment extends Fragment {
		
		public FuelMapSectionFragment() {
			
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			View rootView = inflater.inflate(
					R.layout.fragment_displayprices_map, container, false);	
			return rootView;
		}
	}

}
