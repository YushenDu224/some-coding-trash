package com.hydrodaily.app;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.os.Build;
import java.time.*;

public class ReminderReceiver extends BroadcastReceiver {
    static final String CHANNEL="water";
    static boolean allowed(Context c) {
        return ((NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE)).areNotificationsEnabled()
            && (Build.VERSION.SDK_INT<33 || c.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)==PackageManager.PERMISSION_GRANTED);
    }
    static void schedule(Context c) {
        Store s=new Store(c);
        AlarmManager am=(AlarmManager)c.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pi=PendingIntent.getBroadcast(c,1,new Intent(c,ReminderReceiver.class),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        am.cancel(pi);
        if(!s.p.getBoolean("enabled",false)||!allowed(c)) return;
        ZonedDateTime now=ZonedDateTime.now();
        int start=s.p.getInt("start",8),end=s.p.getInt("end",22),interval=s.p.getInt("interval",60);
        ZonedDateTime next=now.plusMinutes(interval);
        if(now.getHour()<start) next=now.toLocalDate().atTime(start,0).atZone(now.getZone());
        if(next.getHour()>=end||s.total(Store.day())>=s.goal()) next=now.toLocalDate().plusDays(1).atTime(start,0).atZone(now.getZone());
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,next.toInstant().toEpochMilli(),pi);
    }
    @Override public void onReceive(Context c,Intent intent) {
        Store s=new Store(c); int hour=LocalTime.now().getHour();
        if(s.p.getBoolean("enabled",false)&&allowed(c)&&hour>=s.p.getInt("start",8)&&hour<s.p.getInt("end",22)&&s.total(Store.day())<s.goal()) {
            NotificationManager nm=(NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE);
            nm.createNotificationChannel(new NotificationChannel(CHANNEL,"饮水提醒",NotificationManager.IMPORTANCE_DEFAULT));
            PendingIntent open=PendingIntent.getActivity(c,0,new Intent(c,MainActivity.class),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
            nm.notify(10,new Notification.Builder(c,CHANNEL).setSmallIcon(android.R.drawable.ic_dialog_info).setContentTitle("休息一下，喝点水")
                .setContentText("今天已记录 "+s.total(Store.day())+" ml，目标 "+s.goal()+" ml。按需小口饮用。")
                .setContentIntent(open).setAutoCancel(true).build());
        }
        schedule(c);
    }
}
