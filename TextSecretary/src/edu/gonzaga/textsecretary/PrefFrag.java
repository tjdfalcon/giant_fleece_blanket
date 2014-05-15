package edu.gonzaga.textsecretary;

import java.util.ArrayList;

import com.android.vending.billing.IInAppBillingService;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender.SendIntentException;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.util.Log;

public class PrefFrag extends PreferenceFragment implements OnSharedPreferenceChangeListener{
	
	private IInAppBillingService mService;
	private String PACKAGE_NAME;
	private Register task;

	ServiceConnection mServiceConn = new ServiceConnection() {
	   @Override
	   public void onServiceDisconnected(ComponentName name) {
	       mService = null;
	   }

	   @Override
	   public void onServiceConnected(ComponentName name, 
	      IBinder service) {
	       mService = IInAppBillingService.Stub.asInterface(service);
	       
			//set visibility of unlock
	       if(isPurchased()){	//queries purchases
	    	   PreferenceCategory preferenceCategory = (PreferenceCategory) findPreference("Activation");
	    	   getPreferenceScreen().removePreference(preferenceCategory);
	       }
	   }
	};
	
	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);
		
		//set message disability based on calendar selection
		if(getPreferenceManager().getSharedPreferences().getBoolean("calendar_preference", true)){
			Preference messagePreference = findPreference("custom_message_preference");
	        messagePreference.setEnabled(false);
		}
		
		//bind service?
		PACKAGE_NAME = getActivity().getPackageName();
		getActivity().bindService(new 
		        Intent("com.android.vending.billing.InAppBillingService.BIND"),
		                mServiceConn, Context.BIND_AUTO_CREATE);
	}
	
	@Override
	public void onResume(){
	    super.onResume();
	    getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
	}
	@Override
	public void onPause(){
	    super.onPause();
	    getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
	}
	
	@Override
	public void onDestroy() {
	    super.onDestroy();
	    if (mService != null) {
	    	getActivity().unbindService(mServiceConn);
	    }   
	}
	
	public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference key){
		if(key.toString().equals("Unlock Text Secretary")){
			purchaseUnlock();
		}
		return false;
	}
	
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key)
	{
	    if (key.equals("calendar_preference")){
	        Preference preference = findPreference("custom_message_preference");
	        preference.setEnabled(!sharedPreferences.getBoolean("calendar_preference", false));
	    }
	}
	
	//checks if unlock has already been purchased
	private boolean isPurchased(){
		SharedPreferences securePreferences = new SecurePreferences(getActivity().getApplicationContext());
		String account = UserEmailFetcher.getEmail(getActivity().getApplicationContext());
		//check shared preferences first
		if(securePreferences.getBoolean(account+"_paid", false))
			return true;
		//else query google services
		else{
			try {
				Bundle ownedItems = mService.getPurchases(3, PACKAGE_NAME, "inapp", null);
				
				int response = ownedItems.getInt("RESPONSE_CODE");
				if (response == 0) {
				   ArrayList<String> ownedSkus =
				      ownedItems.getStringArrayList("INAPP_PURCHASE_ITEM_LIST");
				   
				   if(ownedSkus.isEmpty())
					   return false;
				   else
					   return true;
				}
			} catch (RemoteException e) {
				Log.e("PURCHASE", "query failed");
				e.printStackTrace();
			}
			return false;
		}
	}
	
	//TODO: encrypt payload??
	private void purchaseUnlock(){
		try {
			String payload = UserEmailFetcher.getEmail(getActivity().getApplicationContext());
			Bundle buyIntentBundle = mService.getBuyIntent(3, PACKAGE_NAME, "text_secretary_unlock", "inapp", payload);
			PendingIntent pendingIntent = buyIntentBundle.getParcelable("BUY_INTENT");
			
			getActivity().startIntentSenderForResult(pendingIntent.getIntentSender(),
					   1001, new Intent(), Integer.valueOf(0), Integer.valueOf(0),
					   Integer.valueOf(0));
		}catch (SendIntentException e) {
			Log.e("PURCHASE", "didn't start activity");
			e.printStackTrace();
		}
		 catch (RemoteException e) {
			Log.e("PURCHASE", "getting intent failed");
			e.printStackTrace();
		}
	}
	
	//securely stores data locally, then stores on server
	private void storeActivation(){
		//securely store in shared preference
		SharedPreferences secureSettings = new SecurePreferences(getActivity().getApplicationContext());
		SharedPreferences.Editor secureEdit = secureSettings.edit();
		String account = UserEmailFetcher.getEmail(getActivity().getApplicationContext());
		
		secureEdit.putBoolean(account+"_paid", true);
		secureEdit.commit();
		
		//store on server
		task = new Register(getActivity().getApplicationContext(), true);
		task.execute();
	}
	
	@Override
	//I'm not sure why this is useful
	public void onActivityResult(int requestCode, int resultCode, Intent data) {          	        
      if (resultCode == Activity.RESULT_OK) {
         try {
            Log.d("PURCHASE", "Purchase completed");
            storeActivation();
          }
          catch (Exception e) {
        	 //task set false?
             Log.e("PURCHASE", "Failed to parse purchase data.");
             e.printStackTrace();
          }
      }
	}
}
