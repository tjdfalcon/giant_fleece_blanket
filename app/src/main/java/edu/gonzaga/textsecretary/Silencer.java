package edu.gonzaga.textsecretary;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.AudioManager;
import android.util.Log;

import java.util.Calendar;

/**
 * Created by Tyler on 1/1/2015.
 */
public class Silencer extends BroadcastReceiver {

    private static final int SILENCER_GARBAGE_MODE = 100;   //used for when silencer is not active
    private static final int CALENDAR_POLL_FREQ = 15 * 60 * 1000;   //15 minutes

    private static final String TAG = "SILENCER";
    private static boolean isSilenced = false;
    private static int prevRingerMode = SILENCER_GARBAGE_MODE;
    private static Context mContext;
    private static Calendar_Service calendarService;

    @Override
    public void onReceive(Context context, Intent intent) {
        //don't schedule if silencer is already on
        if (intent.getAction().equals("edu.gonzaga.text_secretary.silencer.CALENDAR_UPDATE") && !isSilenced) {
            Log.d(TAG, "UPDATE");
            scheduleSilencing();
        }
        else if (intent.getAction().equals("edu.gonzaga.text_secretary.silencer.ENABLE")) {
            Log.d(TAG, "ENABLE");
            silenceRinger();
        }
        else if (intent.getAction().equals("edu.gonzaga.text_secretary.silencer.DISABLE")) {
            Log.d(TAG, "DISABLE");
            restoreRingerMode();
            scheduleSilencing(); //since we're up, let's do an update
        }
    }

    //starts periodic check of calendar for times to enable/disable silencer
    public static void startSilencerPoller(Context context) {
        mContext = context;
        calendarService = new Calendar_Service(mContext);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent updateIntent = new Intent(context, Silencer.class);
        updateIntent.setAction("edu.gonzaga.text_secretary.silencer.CALENDAR_UPDATE");
        PendingIntent updatePendingIntent = PendingIntent.getBroadcast(context, 0, updateIntent, 0);
        alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,System.currentTimeMillis(),CALENDAR_POLL_FREQ,
                updatePendingIntent);

        scheduleSilencing();    //schedule immediately
    }

    //remove all alarms
    public static void stopSilencerPoller() {
        AlarmManager alarmManager = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);

        Intent updateIntent = new Intent(mContext, Silencer.class);
        updateIntent.setAction("edu.gonzaga.text_secretary.silencer.CALENDAR_UPDATE");
        PendingIntent updatePendingIntent = PendingIntent.getBroadcast(mContext, 0, updateIntent, 0);

        Intent enableIntent = new Intent(mContext, Silencer.class);
        enableIntent.setAction("edu.gonzaga.text_secretary.silencer.ENABLE");
        PendingIntent enablePendingIntent = PendingIntent.getBroadcast(mContext, 0, enableIntent, 0);

        Intent disableIntent = new Intent(mContext, Silencer.class);
        enableIntent.setAction("edu.gonzaga.text_secretary.silencer.DISABLE");
        PendingIntent disablePendingIntent = PendingIntent.getBroadcast(mContext, 0, disableIntent, 0);

        alarmManager.cancel(updatePendingIntent);
        alarmManager.cancel(enablePendingIntent);
        alarmManager.cancel(disablePendingIntent);

        restoreRingerMode();
    }

    private static void scheduleSilencing() {
        Cursor cursor = retrieveCalendarEvents();

        //if event exists
        if (cursor.moveToNext()) {
            Log.d(TAG, "event found");
            //get first event info
            long start = cursor.getLong(Calendar_Service.ProjectionAttributes.BEGIN);
            long end = cursor.getLong(Calendar_Service.ProjectionAttributes.END);
            AlarmManager alarmManager = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);

            //schedule silencer enable at start of event
            Intent enableIntent = new Intent(mContext, Silencer.class);
            enableIntent.setAction("edu.gonzaga.text_secretary.silencer.ENABLE");
            PendingIntent enablePendingIntent = PendingIntent.getBroadcast(mContext, 0, enableIntent, 0);
            alarmManager.set(AlarmManager.RTC_WAKEUP,start,enablePendingIntent);

            //schedule silencer shut off at end of event
            Intent disableIntent = new Intent(mContext, Silencer.class);
            enableIntent.setAction("edu.gonzaga.text_secretary.silencer.DISABLE");
            PendingIntent disablePendingIntent = PendingIntent.getBroadcast(mContext, 0, disableIntent, 0);
            alarmManager.set(AlarmManager.RTC_WAKEUP,end,disablePendingIntent);
        }
        else
            Log.d(TAG, "event not found");
        cursor.close();
    }

    private static Cursor retrieveCalendarEvents() {
        Calendar start = Calendar.getInstance();
        Calendar end = Calendar.getInstance();
        end.add(Calendar.MINUTE, 30);
        return calendarService.getCursorForDates(start, end);
    }

    public static void silenceRinger() {
        AudioManager ringerManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        int tempRingerMode = ringerManager.getRingerMode();
        //prevents possible conflicts between listeners
        if (tempRingerMode != AudioManager.RINGER_MODE_SILENT) {
            isSilenced = true;
            Log.d(TAG, tempRingerMode + " silence");
            ringerManager.setRingerMode(AudioManager.RINGER_MODE_SILENT);
            prevRingerMode = tempRingerMode;	//save current mode
        }
    }

    public static void restoreRingerMode() {
        Log.d(TAG, prevRingerMode + " restore");
        //if restore necessary
        if (prevRingerMode != SILENCER_GARBAGE_MODE) {
            AudioManager ringerManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
            ringerManager.setRingerMode(prevRingerMode);
            prevRingerMode = SILENCER_GARBAGE_MODE;   //restore garbage mode
            isSilenced = false;
        }
    }
}
