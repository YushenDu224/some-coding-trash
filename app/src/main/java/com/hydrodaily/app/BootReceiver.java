package com.hydrodaily.app;
import android.content.*;
public class BootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c,Intent i) {
        String action=i.getAction();
        if(Intent.ACTION_BOOT_COMPLETED.equals(action)||Intent.ACTION_TIME_CHANGED.equals(action)||Intent.ACTION_TIMEZONE_CHANGED.equals(action)) ReminderReceiver.schedule(c);
    }
}
