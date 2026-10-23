package vn.dieutuan.dynamicpill;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.service.notification.NotificationListenerService;

/**
 * An explicit, user-enabled foreground service to keep the pill's process alive
 * after the main Activity is dismissed. It is never a guarantee against force-stop.
 */
public class PillKeepAliveService extends Service {
    private static final String CHANNEL="dynamic_pill_active";
    private static final int NOTIFICATION_ID=7318;
    @Override public void onCreate(){
        super.onCreate();
        NotificationManager manager=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        if(manager!=null&&Build.VERSION.SDK_INT>=26){
            NotificationChannel ch=new NotificationChannel(CHANNEL,
                "Dynamic Pill đang chạy",NotificationManager.IMPORTANCE_LOW);
            ch.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            ch.setDescription("Hiển thị trạng thái viên thuốc; chạm để mở cài đặt.");
            manager.createNotificationChannel(ch);
        }
    }
    @Override public int onStartCommand(Intent intent,int flags,int startId){
        SharedPreferences settings=getSharedPreferences(IslandListenerService.PREFS,MODE_PRIVATE);
        if(!settings.getBoolean("enabled",false)||!Settings.canDrawOverlays(this)){
            stopSelf();return START_NOT_STICKY;
        }
        try{
            Intent launch=new Intent(this,MainActivity.class);
            launch.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_SINGLE_TOP);
            int pFlags=PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE;
            PendingIntent pi=PendingIntent.getActivity(this,0,launch,pFlags);
            Notification note=new Notification.Builder(this,CHANNEL)
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle("Dynamic Pill Pro đang hoạt động")
                .setContentText("Chạm để điều chỉnh viên thuốc • Chạy nền")
                .setCategory(Notification.CATEGORY_SERVICE)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setOngoing(true)
                .setContentIntent(pi)
                .build();
            if(Build.VERSION.SDK_INT>=34)startForeground(NOTIFICATION_ID,note,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
            else startForeground(NOTIFICATION_ID,note);
            try{NotificationListenerService.requestRebind(
                new ComponentName(this,IslandListenerService.class));}catch(Exception ignored){}
            sendBroadcast(new Intent(IslandListenerService.ACTION_REFRESH).setPackage(getPackageName()));
            return START_STICKY;
        }catch(Exception ex){
            stopSelf();return START_NOT_STICKY;
        }
    }
    @Override public IBinder onBind(Intent intent){return null;}
}