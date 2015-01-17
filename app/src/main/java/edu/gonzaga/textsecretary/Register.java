package edu.gonzaga.textsecretary;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.Log;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class Register extends AsyncTask<Boolean, Void, Boolean> {
	
	private static final String TAG = "REGISTER";
	private Context mContext;
	private boolean problemHappened = false;
	
    // JSON parser class
    private JSONParser jsonParser = new JSONParser();

    //server URL:
    private static final String LOGIN_URL = "http://collinwalker.guru/textsecretary/register.php";

    //ids
    private static final String TAG_PAID = "paid";
    private static final String TAG_DATE = "trialEND";
	
    public Register (Context context){
         mContext = context;
    }

	@Override
	protected Boolean doInBackground(Boolean... pay) {
		String paid;
		String date;
		String userEmail = UserEmailFetcher.getEmail(mContext);
		String serverPay;
		JSONObject json;
		int count = 0;
		
		if(pay[0]) {
			storeActivation(mContext);	//storing here is probably a little safer
        	paid = "1";
		}
        else
        	paid = "0";
        
        // Building Parameters
        List<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("userIDD", userEmail));
        params.add(new BasicNameValuePair("just_paid", paid));
        
        //try up to 5 times
        while (count < 5) {
	        try {
	            Log.d(TAG, "starting attempt " + count);
	
	            //Posting user data to script
				json = jsonParser.makeHttpRequest(
					       LOGIN_URL, "POST", params);
	
	            Log.d(TAG, "connected");
	            Log.d(TAG, "JSON response" + json.toString());
	            
	            //retrieve values
	        	serverPay = json.getString(TAG_PAID);
	        	date = json.getString(TAG_DATE);
	        	
	        	//if we get this far, then everything should have worked and checks can be performed
	        	return checkActivation(date, serverPay, pay[0]);
	        } 
	        catch (Exception e){
	        	//bad so increment count and try again
	        	Log.e(TAG, "CATCH httpRequest " + e.getMessage());
	        	count++;
	        }
        }
        
        //user is screwed so set boolean to show message
        problemHappened = true;
        return false;
	}
	
	protected void onPostExecute(final Boolean result){
		//if error show dialogue
        if (mContext instanceof Activity) {
			((Activity) mContext).runOnUiThread(new Runnable() {
	
				@Override
				public void run() {
					if(!result && problemHappened) {
                        showErrorDialogue();
				    }
				}
				
			});
		}
	}
    
	//ensures current date is within server trial dates
	//firstActivation determines if this is first time being activated and will therefore skip the storing process this time
    private boolean checkActivation(String date, String serverPay, boolean firstActivation){
    	Log.d(TAG, serverPay);
    	//if not paid
    	if (!serverPay.equals("1")){
    		boolean inTrial = false;
		    if (date != null){
		    	SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
		    	String currentDateandTime = sdf.format(new Date());
		    	Log.d(TAG, date + "  " + currentDateandTime);
		    	inTrial =  isInTrialDate(date, currentDateandTime);
		    	if(inTrial)
		    		storeTrialInfo(date);
		    }
		    
		    return inTrial;
       }
    	//must have paid
    	else{
    		Log.d(TAG, "paid");
    		//skips the storing procedure, as it would have already been stored
    		if (!firstActivation)
    			storeActivation(mContext);
    		return true;
    	}
    }
    
    //securely stores trial information
    private void storeTrialInfo(String endTrial){
    	SharedPreferences secureSettings = new SecurePreferences(mContext);
    	String account = UserEmailFetcher.getEmail(mContext);
    	
    	//if date preference doesn't exist, create one
    	if(secureSettings.getLong(account+"_trial", 0) == 0){
    		try{
	    		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
	            Date endDate = sdf.parse(endTrial);
	    		
	    		SharedPreferences.Editor secureEdit = secureSettings.edit();
				secureEdit.putLong(account+"_trial", endDate.getTime());
				secureEdit.apply();
    		}
    		catch(ParseException e){
    			Log.e(TAG, "parse exception");
    			Log.e(TAG, e.getMessage());
    		}
    	}
    }
    
	//securely stores data locally
	private static void storeActivation(Context context){
		//securely store in shared preference
		SharedPreferences secureSettings = new SecurePreferences(context);
		String account = UserEmailFetcher.getEmail(context);
		
		//update preference
		SharedPreferences.Editor secureEdit = secureSettings.edit();
		secureEdit.putBoolean(account+"_paid", true);
		secureEdit.apply();
	}
    
    private static boolean isInTrialDate(String endTrial, String currentDate) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
            Date end = sdf.parse(endTrial);
            Date current = sdf.parse(currentDate);

            if(end.compareTo(current)>0){
                Log.v(TAG,"end is after current");
            	return true;
            }else if(end.compareTo(current)<=0){
                Log.v(TAG,"end is before current");
            	return false;
            }
        } catch(Exception e) {
        	Log.d(TAG, "CATCH compare " + e.toString());
        }
        return false;
    }
    
    //shows error if server is not responding correctly
    private void showErrorDialogue() {
    	try {
	    	new AlertDialog.Builder(mContext)
		    .setTitle("Something unfortunate happened...")
		    .setMessage("Your device was not able to verify activation.  Please check your internet connection and ensure you are using the latest version of the application.")
		    .setNeutralButton("Close", new DialogInterface.OnClickListener() {
		        public void onClick(DialogInterface dialog, int which) {
		        	dialog.dismiss();
		        }
		     })
		     .show();
    	} catch (Exception e) {
    		//not in main activity, not a problem
			Log.w(TAG, e.getMessage());
    	}
    }
}