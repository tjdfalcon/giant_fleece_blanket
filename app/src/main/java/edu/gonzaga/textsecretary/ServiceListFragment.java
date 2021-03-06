package edu.gonzaga.textsecretary;

import android.app.ListFragment;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


public class ServiceListFragment extends ListFragment {
	private static final String[] from = {"name", "purpose"};
	private static final int[] to = {R.id.text1, R.id.text2};
	private SimpleAdapter adapter, adapteroff;
	private ArrayList<Map<String, String>> list;
	private SharedPreferences settings;

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		settings = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
		buildData();

		adapter = new SimpleAdapter(getActivity(), list,
				R.layout.list_fragment_layout, from, to);

		adapteroff = new SimpleAdapter(getActivity(), list,
				R.layout.list_fragment_layout_off, from, to);

		setListAdapter(adapter);
		Log.d("TAG", "set adapter");
	}

	@Override
	public void onResume() {
		super.onResume();

		list.clear();
		Log.d("TAG", "cleared adapter");
		buildData();
		Log.d("TAG", "setValues");

		adapter = new SimpleAdapter(getActivity(), list,
				R.layout.list_fragment_layout, from, to);

		adapteroff = new SimpleAdapter(getActivity(), list,
				R.layout.list_fragment_layout_off, from, to);

		if (settings.getBoolean("smsState", true)) {
			setListAdapter(adapter);
		} else {
			setListAdapter(adapteroff);
		}
	}

	public void changeTextColor(boolean dark) {
		if (dark) {
			setListAdapter(adapter);
		} else
			setListAdapter(adapteroff);
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		startActivity(new Intent(getActivity(), SettingsActivity.class));
	}

	private ArrayList<Map<String, String>> buildData() {
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
		list = new ArrayList<>();

		//respond to
		String respondTo = preferences.getString("respond_to_preference", "2");
		switch (respondTo) {
			case "0":
				list.add(putData("Replying to", "Texts"));
				break;
			case "1":
				list.add(putData("Replying to", "Calls"));
				break;
			case "2":
				list.add(putData("Replying to", "Calls"));
				break;
			case "3":
				list.add(putData("Replying to", "Texts & Calls"));
				break;
			case "4":
				list.add(putData("Replying to", "Texts & Calls"));
				break;
		}

		//Calendar
		if (preferences.getBoolean("calendar_preference", true))
			list.add(putData("Calendar", "ON"));
		else
			list.add(putData("Calendar", "OFF"));

		//Driving
		if (preferences.getBoolean("driving_preference", true))
			list.add(putData("Driving Detection", "ON"));
		else
			list.add(putData("Driving Detection", "OFF"));

		//Do Not Disturb
		if (preferences.getBoolean("silence_preference", false))
			list.add(putData("Do Not Disturb", "ON"));
		else
			list.add(putData("Do Not Disturb", "OFF"));

		//Start on Boot
		if (preferences.getBoolean("start_on_boot_preference", false))
			list.add(putData("Start on Boot", "ON"));
		else
			list.add(putData("Start on Boot", "OFF"));

		//Notification
		if (preferences.getBoolean("notification_preference", true))
			list.add(putData("Notifications", "ON"));
		else
			list.add(putData("Notifications", "OFF"));

		//Single Response
		long singleResponse = Long.valueOf(preferences.getString("single_response_preference", "0"));
		if (singleResponse == 0)
			list.add(putData("Single Response", "Off"));
		else
			list.add(putData("Single Response", Long.valueOf(preferences.getString("list_preference", "1800000")) / 60000 + " minutes"));

		return list;
	}

	private HashMap<String, String> putData(String name, String purpose) {
		HashMap<String, String> item = new HashMap<>();
		item.put("name", name);
		item.put("purpose", purpose);
		return item;
	}
}
