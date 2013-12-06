package com.benjamininnovations.fuelwatcher;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.widget.SimpleCursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.benjamininnovations.fuelwatcher.DisplayPrices.MyOnClickListener;

/**
 * A simple {@link android.support.v4.app.Fragment} subclass. Activities that
 * contain this fragment must implement the
 * {@link FuelListFragment.OnFragmentInteractionListener} interface to handle
 * interaction events. Use the {@link FuelListFragment#newInstance} factory
 * method to create an instance of this fragment.
 * 
 */
public class FuelListFragment extends ListFragment {
	
	private OnFragmentInteractionListener mListener;
	private static Cursor mCursor;

	/**
	 * Use this factory method to create a new instance of this fragment using
	 * the provided parameters.
	 */
	// TODO: Rename and change types and number of parameters
	public static FuelListFragment newInstance(Cursor cursor) {
		FuelListFragment fragment = new FuelListFragment();
		Bundle args = new Bundle();
		fragment.setArguments(args);
		mCursor = cursor;
		return fragment;
	}

	public FuelListFragment() {
		// Required empty public constructor
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		// Inflate the layout for this fragment

		View rootView = inflater.inflate(
				R.layout.fragment_display_prices_dummy, container, false);
		Context context = rootView.getContext();
		
		String[] from = new String[] {"title"};
		int[] to = new int[] {android.R.id.text1};
		
		
		SimpleCursorAdapter adapt = new SimpleCursorAdapter(context, android.R.layout.simple_list_item_1, mCursor, from, to, 0);
		setListAdapter(adapt);
//		fuelView.setAdapter(adapt);
		
//		MyOnClickListener listener = new MyOnClickListener();
//		fuelView.setOnItemClickListener(listener);
		
		return inflater.inflate(R.layout.fragment_fuel_list, container, false);
	}

	// TODO: Rename method, update argument and hook method into UI event
	public void onButtonPressed(Uri uri) {
		if (mListener != null) {
			mListener.onFragmentInteraction(uri);
		}
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		try {
			mListener = (OnFragmentInteractionListener) activity;
		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString()
					+ " must implement OnFragmentInteractionListener");
		}
	}

	@Override
	public void onDetach() {
		super.onDetach();
		mListener = null;
	}

	/**
	 * This interface must be implemented by activities that contain this
	 * fragment to allow an interaction in this fragment to be communicated to
	 * the activity and potentially other fragments contained in that activity.
	 * <p>
	 * See the Android Training lesson <a href=
	 * "http://developer.android.com/training/basics/fragments/communicating.html"
	 * >Communicating with Other Fragments</a> for more information.
	 */
	public interface OnFragmentInteractionListener {
		// TODO: Update argument type and name
		public void onFragmentInteraction(Uri uri);
	}

}
