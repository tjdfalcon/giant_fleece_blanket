package edu.gonzaga.textsecretary.activity_recognition;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.DetectedActivity;

import edu.gonzaga.textsecretary.Silencer;
import edu.gonzaga.textsecretary.activity_recognition.ActivityUtils.REQUEST_TYPE;

public class ActivityRecognizer {
	
	private static volatile ActivityRecognizer instance;    //instance of self
    protected boolean wasDriving;
    protected int drivingConfidence;     // -2 <= dC <= 3
	private REQUEST_TYPE mRequestType;
	private DetectionRequester mDetectionRequester;
    private DetectionRemover mDetectionRemover;
    private Context mContext;
    private SharedPreferences prefs;


    private ActivityRecognizer (Context context) {
        mContext = context;
    }

    public static ActivityRecognizer getInstance(Context context) {
        if (instance == null) {
            synchronized (ActivityRecognizer.class) {
                if (instance == null) {
                    instance = new ActivityRecognizer(context);
                }
            }
        }

        return instance;
    }
    
    private boolean servicesConnected() {
        // Check that Google Play services is available
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(mContext);

        //return Google Services availability
        return ConnectionResult.SUCCESS == resultCode;
    }
	
	//start listening for updates
	public void startUpdates() {
		wasDriving = false;
        drivingConfidence = 0;
		mDetectionRequester = new DetectionRequester(mContext);
        mDetectionRemover = new DetectionRemover(mContext);
        
        // Check for Google Play services
        if (!servicesConnected()) {
            return;
        }

        /*
         * Set the request type. If a connection error occurs, and Google Play services can
         * handle it, then onActivityResult will use the request type to retry the request
         */
        mRequestType = ActivityUtils.REQUEST_TYPE.ADD;

        // Pass the update request to the requester object
        mDetectionRequester.requestUpdates();
        Log.d(ActivityUtils.APPTAG, "started driving service");
    }
	
	//stop checking for activity updates
	public void stopUpdates() {
        // Check for Google Play services
        if (!servicesConnected()) {
            return;
        }

        /*
         * Set the request type. If a connection error occurs, and Google Play services can
         * handle it, then onActivityResult will use the request type to retry the request
         */
        mRequestType = ActivityUtils.REQUEST_TYPE.REMOVE;

        // Pass the remove request to the remover object
        mDetectionRemover.removeUpdates(mDetectionRequester.getRequestPendingIntent());

        /*
         * Cancel the PendingIntent. Even if the removal request fails, canceling the PendingIntent
         * will stop the updates.
         */
        mDetectionRequester.getRequestPendingIntent().cancel();
        
        //kill any remaining notification
        ((NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE)).cancel(11001100);
        //ensure ringer is restored if necessary
        if (isDriving())
            Silencer.getInstance(mContext).restoreRingerMode();
        Log.d(ActivityUtils.APPTAG, "stopped driving service");
    }

    protected void raiseLowerDrivingConfidence(int activityType) {
        prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        boolean isPassenger = prefs.getBoolean("isPassenger", false);

        if(isPassenger){
            if(activityType == DetectedActivity.STILL || activityType == DetectedActivity.ON_FOOT){
                if(drivingConfidence < 0)
                    drivingConfidence += 1;
            }

            else if(activityType == DetectedActivity.IN_VEHICLE || activityType == DetectedActivity.ON_BICYCLE){
                drivingConfidence = -3;
            }
        }

        else{
            if (((activityType == DetectedActivity.IN_VEHICLE) || (activityType == DetectedActivity.ON_BICYCLE)) && (drivingConfidence < 3))
                drivingConfidence += 1;

            else if ((activityType != DetectedActivity.IN_VEHICLE) && (activityType != DetectedActivity.ON_BICYCLE) && (activityType != DetectedActivity.UNKNOWN) && (drivingConfidence > -3))
                drivingConfidence -= 1;
        }
/*
        //if driving
        if (((activityType == DetectedActivity.IN_VEHICLE) || (activityType == DetectedActivity.ON_BICYCLE)) && (drivingConfidence < 3))
            drivingConfidence += 1;
        //if not driving and not unknown
        else if ((activityType != DetectedActivity.IN_VEHICLE) && (activityType != DetectedActivity.ON_BICYCLE) && (activityType != DetectedActivity.UNKNOWN) && (drivingConfidence > -2))
            drivingConfidence -= 1;
*/
        Log.d(ActivityUtils.APPTAG, "driving confidence: " + drivingConfidence);
    }
	
	/*
     * Handle results returned to this Activity by other Activities started with
     * startActivityForResult(). In particular, the method onConnectionFailed() in
     * DetectionRemover and DetectionRequester may call startResolutionForResult() to
     * start an Activity that handles Google Play services problems. The result of this
     * call returns here, to onActivityResult.
     */
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {

        // Choose what to do based on the request code
        switch (requestCode) {

            // If the request code matches the code sent in onConnectionFailed
            case ActivityUtils.CONNECTION_FAILURE_RESOLUTION_REQUEST :

                switch (resultCode) {
                    // If Google Play services resolved the problem
                    case Activity.RESULT_OK:

                        // If the request was to start activity recognition updates
                        if (ActivityUtils.REQUEST_TYPE.ADD == mRequestType) {

                            // Restart the process of requesting activity recognition updates
                            mDetectionRequester.requestUpdates();

                        // If the request was to remove activity recognition updates
                        } else if (ActivityUtils.REQUEST_TYPE.REMOVE == mRequestType ){

                                /*
                                 * Restart the removal of all activity recognition updates for the 
                                 * PendingIntent.
                                 */
                                mDetectionRemover.removeUpdates(
                                    mDetectionRequester.getRequestPendingIntent());

                        }
                    break;

                    // If any other result was returned by Google Play services
                    default:

                        // Report that Google Play services was unable to resolve the problem.
                        Log.e(ActivityUtils.APPTAG, "google play couldn't do anything useful");
                }

            // If any other request code was received
            default:
               // Report that this Activity received an unknown requestCode
               Log.e(ActivityUtils.APPTAG, "I'm not sure what happened but it wasn't good");

               break;
        }
    }

    public boolean isDriving() {
        return drivingConfidence > 0;
    }
}
