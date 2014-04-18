package edu.gonzaga.textsecretary;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.util.Log;

public class SMS_Service extends Service{
	
	final long REPEAT_TIME = 1800000;	//30 minutes
	String TAG = "TAG";
	Calendar_Service calendar;
	String message = "Sorry, I'm busy. I'll get back to you as soon as possible.";
	final Notification_Service mnotification = new Notification_Service(SMS_Service.this);
	HashMap<String, Long> recentNumbers = new HashMap<String, Long>();
	
	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}

	@Override
	public void onCreate(){
		super.onCreate();
		IntentFilter filter = new IntentFilter();
		filter.addAction("android.provider.Telephony.SMS_RECEIVED");		

		calendar = new Calendar_Service(SMS_Service.this);
		registerReceiver (smsListener, filter);
	}
	
	@Override
	public void onDestroy(){
		super.onDestroy();
		unregisterReceiver(smsListener);
	}
	
	private BroadcastReceiver smsListener = new BroadcastReceiver(){
		@Override
		public void onReceive(Context context, Intent intent) {
			if(intent.getAction().equals("android.provider.Telephony.SMS_RECEIVED") && calendar.inEvent()){
				Bundle bundle = intent.getExtras();
				SmsMessage[] msgs = null;
				String msg_from = "empty";
				Log.d(TAG, "we're here");
				if (bundle != null){
					try{
						Log.d(TAG, "trying");
						Object[] pdus = (Object[]) bundle.get("pdus");
	                    msgs = new SmsMessage[pdus.length];
	    				Log.d(TAG, "created msgs");
						for(int i = 0; i < msgs.length; i++){
							Log.d(TAG, "in the for");
	                        msgs[i] = SmsMessage.createFromPdu((byte[])pdus[i]);
	                        msg_from = msgs[i].getOriginatingAddress();
	                        //String msgBody = msgs[i].getMessageBody();
	        				
	        				if (!isRecent(msg_from)){
	        					SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
	        			        message = prefs.getString("custom_message_preference", message);
	        			        Log.d("SDF", message);
	        			        String newMessage = getNewMessage(message);
	        					sendSMS(msg_from, newMessage);
		        				storeMessage(msg_from, newMessage);
 		        				final String savefrom = msg_from;
 		        				final String savemessage = newMessage;
 		        				
	        				    new Handler().postDelayed(new Runnable() {
 	        				        @Override
 	        				        public void run() {
										storeMessage(savefrom , savemessage);
 	        				        }
 	        				    }, 2000);
	        				}
	                    }
					} catch(Exception e){
						Log.d(TAG, "cought");
					}
				}
			}
		}
	};
	
	private boolean isRecent(String phoneNumber){
		//if number is new
		long currentTime = Calendar.getInstance().getTimeInMillis();
		if (!recentNumbers.containsKey(phoneNumber)){
			recentNumbers.put(phoneNumber, currentTime);
			return false;
		}
		//if number is already in map
		else{
			long time = recentNumbers.get(phoneNumber);
			recentNumbers.put(phoneNumber, currentTime);
			if ((currentTime - time) > REPEAT_TIME){
				return false;
			}
			else{
				Log.d("SDF", "recent");
				return true;
			}
		}
	}
	
	private String getNewMessage(String oldMessage){
		String newMessage;
		
		newMessage = oldMessage.replace("[end]", getDate(calendar.getEventEnd()));
		newMessage = newMessage.replace("[name]", calendar.getEventName());
		return newMessage;
	}
	
	private String getDate (long date){
		SimpleDateFormat dateFormat = new SimpleDateFormat ("hh:mm a", Locale.US);
		Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(date);
		return dateFormat.format (calendar.getTime());
	}
	
	private void sendSMS(String phoneNumber, String message){
		SmsManager sms = SmsManager.getDefault();
		sms.sendTextMessage(phoneNumber, null, message, null, null);
        mnotification.displayNotification(phoneNumber);
	}
	
    private void storeMessage(String mobNo, String msg) { 
    	ContentValues values = new ContentValues(); 
    	values.put("address", mobNo); 
    	values.put("body", msg); 
    	getContentResolver().insert(Uri.parse("content://sms/sent"), values); 
  } 
	
}
