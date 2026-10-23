package vn.dieutuan.dynamicpill;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings;

public class BootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context,Intent intent){
        if(intent==null)return;
        String action=intent.getAction();
        if(!Intent.ACTION_BOOT_COMPLETED.equals(action)&&
           !Intent.ACTION_MY_PACKAGE_REPLACED.equals(action))return;
        boolean enabled=context.getSharedPreferences(IslandListenerService.PREFS,Context.MODE_PRIVATE)
            .getBoolean("enabled",false);
        if(!enabled||!Settings.canDrawOverlays(context))return;
        try{
            Intent service=new Intent(context,PillKeepAliveService.class);
            if(Build.VERSION.SDK_INT>=26)context.startForegroundService(service);
            else context.startService(service);
        }catch(Exception ignored){
            // OEM auto-start / foreground-service policy may block reboot start.
        }
    }
}