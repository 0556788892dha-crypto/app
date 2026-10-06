package com.dabarber.app;

import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.os.Build;

public class ReminderReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context,Intent intent){
        String client=intent.getStringExtra("client");
        String date=intent.getStringExtra("date");
        String time=intent.getStringExtra("time");
        String title="תזכורת לתור";
        String message=(client==null||client.isEmpty()?"":client)+"  "+(time==null?"":time)+(date==null||date.isEmpty()?"":" • "+date);
        PendingIntent pi=PendingIntent.getActivity(context,77,new Intent(context,MainActivity.class),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder b=Build.VERSION.SDK_INT>=26?new Notification.Builder(context,"da_barber_reminders"):new Notification.Builder(context);
        b.setSmallIcon(android.R.drawable.ic_menu_today).setContentTitle(title).setContentText(message.trim()).setAutoCancel(true).setContentIntent(pi).setStyle(new Notification.BigTextStyle().bigText(message.trim()));
        ((NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE)).notify((int)(System.currentTimeMillis()&0x7fffffff),b.build());
    }
}
