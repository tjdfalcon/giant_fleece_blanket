package edu.gonzaga.textsecretary;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.ImageView;
import android.widget.RemoteViews;

public class Widget extends AppWidgetProvider {
	SharedPreferences settings;
	Boolean state = true;
	private boolean SMS_Service_State;
	ImageView widgetIcon;
	
    @Override
    public void onEnabled(Context context){
        RemoteViews remoteViews;
        remoteViews = new RemoteViews(context.getPackageName(), R.layout.widgetlayout);
        settings = PreferenceManager.getDefaultSharedPreferences(context);
        SMS_Service_State = settings.getBoolean("smsState", true);
        
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        ComponentName widget = new ComponentName(context, Widget.class);
        
        if(SMS_Service_State)
        	remoteViews.setImageViewResource(R.id.imageview_icon, R.drawable.widgeton);
        
        else
        	remoteViews.setImageViewResource(R.id.imageview_icon, R.drawable.widgetoff);
        
        appWidgetManager.updateAppWidget(widget, remoteViews);

    }
    
    private static final String SYNC_CLICKED    = "automaticWidgetSyncButtonClick";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        RemoteViews remoteViews;
        ComponentName watchWidget;
        
        remoteViews = new RemoteViews(context.getPackageName(), R.layout.widgetlayout);
        watchWidget = new ComponentName(context, Widget.class);

        remoteViews.setOnClickPendingIntent(R.id.imageview_icon, getPendingSelfIntent(context, SYNC_CLICKED));
        
        settings = PreferenceManager.getDefaultSharedPreferences(context);
        SMS_Service_State = settings.getBoolean("smsState", true);

        if(SMS_Service_State)
        	remoteViews.setImageViewResource(R.id.imageview_icon, R.drawable.widgeton);
        
        else
        	remoteViews.setImageViewResource(R.id.imageview_icon, R.drawable.widgetoff);
        
        appWidgetManager.updateAppWidget(watchWidget, remoteViews);
        
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        // TODO Auto-generated method stub
        super.onReceive(context, intent);
        Intent serviceIntent = new Intent(context, SMS_Service.class);
        
        settings = PreferenceManager.getDefaultSharedPreferences(context);
    	SharedPreferences.Editor editor = settings.edit();

        SMS_Service_State = settings.getBoolean("smsState", true);
        
        if (SYNC_CLICKED.equals(intent.getAction())) {

            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);

            RemoteViews remoteViews;
            ComponentName textWidget;

            remoteViews = new RemoteViews(context.getPackageName(), R.layout.widgetlayout);
            textWidget = new ComponentName(context, Widget.class);

            //remoteViews.setTextViewText(R.id.sync_button, "TESTING");
            if(SMS_Service_State){
            	Log.d("TAG", "service on");
            	remoteViews.setImageViewResource(R.id.imageview_icon, R.drawable.widgetoff);
            	context.stopService(serviceIntent);
            	Log.d("TAG", "STOPPED SERVICE");
		        SMS_Service_State = false;
            	editor.putBoolean("smsState", SMS_Service_State);
            }
            
            else{
            	Log.d("TAG","service off");
            	remoteViews.setImageViewResource(R.id.imageview_icon, R.drawable.widgeton);
            	context.startService(serviceIntent);
            	Log.d("TAG", "STARTED SERVICE");
		        SMS_Service_State = true;
            	editor.putBoolean("smsState", SMS_Service_State);
           	}
            
            appWidgetManager.updateAppWidget(textWidget, remoteViews);
        	editor.commit();

        }
    }

	protected PendingIntent getPendingSelfIntent(Context context, String action) {
        Intent intent = new Intent(context, getClass());
        intent.setAction(action);
        return PendingIntent.getBroadcast(context, 0, intent, 0);
    }


}