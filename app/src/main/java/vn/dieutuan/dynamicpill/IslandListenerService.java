package vn.dieutuan.dynamicpill;

import android.app.Notification;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

public class IslandListenerService extends NotificationListenerService {
    public static final String PREFS="dynamic_pill_prefs";
    public static final String ACTION_REFRESH="vn.dieutuan.dynamicpill.REFRESH";
    public static final String ACTION_PREVIEW="vn.dieutuan.dynamicpill.PREVIEW";

    private static final int WHITE=0xFFF7F8FA, MUTED=0xFF9BA6B5, GREEN=0xFF63E6BE;
    private final Handler ui=new Handler(Looper.getMainLooper());
    private SharedPreferences prefs;
    private WindowManager windows;
    private LinearLayout pill, compact, details, controls;
    private TextView compactIcon, compactLabel, titleView, subtitleView, playButton;
    private WindowManager.LayoutParams layout;
    private MediaSessionManager sessionManager;
    private MediaController controller;
    private MediaController.Callback controllerCallback;
    private MediaSessionManager.OnActiveSessionsChangedListener sessionCallback;
    private BroadcastReceiver commands, batteryReceiver;
    private boolean attached=false, expanded=false, listenersSet=false;
    private boolean charging=false;
    private int batteryPercent=0, notificationSequence=0;
    private String currentApp="Thông báo", currentTitle="", currentText="";
    private String ephemeralIcon="", ephemeralText="", ephemeralDetail="";

    @Override public void onListenerConnected(){
        super.onListenerConnected();
        ui.post(()->{
            if(prefs==null)prefs=getSharedPreferences(PREFS,MODE_PRIVATE);
            installReceivers();
            setupSessions();
            refresh();
        });
    }
    @Override public void onListenerDisconnected(){
        ui.post(this::release);
        super.onListenerDisconnected();
    }
    @Override public void onDestroy(){
        release();
        super.onDestroy();
    }
    private int dp(float x){return (int)(x*getResources().getDisplayMetrics().density+0.5f);}
    private GradientDrawable shape(int fill,int radius){
        GradientDrawable d=new GradientDrawable();d.setColor(fill);
        d.setCornerRadius(dp(radius));d.setStroke(dp(1),0xFF232A35);return d;
    }
    private TextView text(String s,int size,int color,boolean bold){
        TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);
        if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        t.setSingleLine(true);t.setEllipsize(TextUtils.TruncateAt.END);return t;
    }
    private LinearLayout row(){LinearLayout r=new LinearLayout(this);r.setOrientation(0);r.setGravity(Gravity.CENTER_VERTICAL);return r;}
    private LinearLayout col(){LinearLayout c=new LinearLayout(this);c.setOrientation(1);return c;}
    private void installReceivers(){
        if(listenersSet)return;
        commands=new BroadcastReceiver(){
            @Override public void onReceive(Context context, Intent intent){
                if(ACTION_REFRESH.equals(intent.getAction()))refresh();
                else if(ACTION_PREVIEW.equals(intent.getAction())){
                    ensurePill();
                    transientMessage("✦","Viên thuốc đang hoạt động","Chạm để mở rộng",5000);
                }
            }
        };
        IntentFilter cmd=new IntentFilter();cmd.addAction(ACTION_REFRESH);cmd.addAction(ACTION_PREVIEW);
        if(Build.VERSION.SDK_INT>=33)registerReceiver(commands,cmd,Context.RECEIVER_NOT_EXPORTED);
        else registerReceiver(commands,cmd);
        batteryReceiver=new BroadcastReceiver(){
            @Override public void onReceive(Context context, Intent intent){
                if(!Intent.ACTION_BATTERY_CHANGED.equals(intent.getAction()))return;
                int level=intent.getIntExtra(BatteryManager.EXTRA_LEVEL,0);
                int scale=intent.getIntExtra(BatteryManager.EXTRA_SCALE,100);
                int percent=scale>0?Math.round(level*100f/scale):0;
                int plugged=intent.getIntExtra(BatteryManager.EXTRA_PLUGGED,0);
                boolean now=plugged!=0;
                boolean changed=now!=charging;
                charging=now;batteryPercent=percent;
                if(changed&&now)transientMessage("⚡","Đang sạc • "+percent+"%","Pin đang được sạc",5000);
            }
        };
        registerReceiver(batteryReceiver,new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        listenersSet=true;
    }
    private void setupSessions(){
        if(sessionManager!=null)return;
        sessionManager=(MediaSessionManager)getSystemService(Context.MEDIA_SESSION_SERVICE);
        if(sessionManager==null)return;
        sessionCallback=sessions->ui.post(()->selectSession(sessions));
        ComponentName listener=new ComponentName(this,IslandListenerService.class);
        try{
            sessionManager.addOnActiveSessionsChangedListener(sessionCallback,listener,ui);
            selectSession(sessionManager.getActiveSessions(listener));
        }catch(SecurityException ex){
            // The user may not have granted/retained notification listener access yet.
        }catch(Exception ignored){}
    }
    private void selectSession(List<MediaController> active){
        MediaController chosen=null;
        if(active!=null){
            for(MediaController c:active){
                PlaybackState state=c.getPlaybackState();
                if(state!=null&&state.getState()==PlaybackState.STATE_PLAYING){chosen=c;break;}
            }
            if(chosen==null){
                for(MediaController c:active){if(c.getMetadata()!=null){chosen=c;break;}}
            }
        }
        if(controller!=null&&controllerCallback!=null){
            try{controller.unregisterCallback(controllerCallback);}catch(Exception ignored){}
        }
        controller=chosen;
        if(chosen!=null){
            controllerCallback=new MediaController.Callback(){
                @Override public void onMetadataChanged(MediaMetadata m){ui.post(IslandListenerService.this::updatePill);}
                @Override public void onPlaybackStateChanged(PlaybackState p){ui.post(IslandListenerService.this::updatePill);}
                @Override public void onSessionDestroyed(){ui.post(IslandListenerService.this::refreshMedia);}
            };
            try{chosen.registerCallback(controllerCallback,ui);}catch(Exception ignored){}
        }
        updatePill();
    }
    private void refreshMedia(){
        if(sessionManager==null)return;
        try{selectSession(sessionManager.getActiveSessions(new ComponentName(this,IslandListenerService.class)));}
        catch(Exception ignored){updatePill();}
    }
    private void refresh(){
        if(prefs==null)prefs=getSharedPreferences(PREFS,MODE_PRIVATE);
        if(prefs.getBoolean("enabled",false)&&Settings.canDrawOverlays(this)){
            ensurePill();refreshMedia();updatePill();
        }else removePill();
    }
    private void ensurePill(){
        if(prefs==null)prefs=getSharedPreferences(PREFS,MODE_PRIVATE);
        if(attached||!prefs.getBoolean("enabled",false)||!Settings.canDrawOverlays(this))return;
        try{
            windows=(WindowManager)getSystemService(WINDOW_SERVICE);
            if(windows==null)return;
            pill=col();pill.setGravity(Gravity.CENTER);pill.setPadding(dp(12),dp(7),dp(12),dp(7));
            pill.setElevation(dp(12));pill.setBackground(shape(0xFF050607,30));
            compact=row();compact.setGravity(Gravity.CENTER_VERTICAL);
            compactIcon=text("♪",17,GREEN,true);
            LinearLayout.LayoutParams ip=new LinearLayout.LayoutParams(dp(24),-2);
            compact.addView(compactIcon,ip);
            compactLabel=text("Dynamic Pill",12,WHITE,true);
            compact.addView(compactLabel,new LinearLayout.LayoutParams(0,-2,1));
            compact.setOnClickListener(v->{expanded=!expanded;updatePill();});
            pill.addView(compact,new LinearLayout.LayoutParams(-1,dp(30)));
            details=col();details.setVisibility(View.GONE);
            details.setPadding(dp(6),dp(10),dp(6),dp(3));
            titleView=text("",15,WHITE,true);subtitleView=text("",12,MUTED,false);
            LinearLayout.LayoutParams titleParams=new LinearLayout.LayoutParams(-1,-2);
            titleParams.bottomMargin=dp(5);details.addView(titleView,titleParams);
            LinearLayout.LayoutParams detailParams=new LinearLayout.LayoutParams(-1,-2);
            detailParams.bottomMargin=dp(12);details.addView(subtitleView,detailParams);
            controls=row();controls.setGravity(Gravity.CENTER);
            TextView back=control("⏮");playButton=control("⏸");TextView next=control("⏭");
            controls.addView(back,new LinearLayout.LayoutParams(0,dp(38),1));
            controls.addView(playButton,new LinearLayout.LayoutParams(0,dp(38),1));
            controls.addView(next,new LinearLayout.LayoutParams(0,dp(38),1));
            back.setOnClickListener(v->sendMedia(0));
            playButton.setOnClickListener(v->sendMedia(1));
            next.setOnClickListener(v->sendMedia(2));
            details.addView(controls,new LinearLayout.LayoutParams(-1,-2));
            pill.addView(details,new LinearLayout.LayoutParams(-1,-2));
            pill.setOnClickListener(v->{expanded=!expanded;updatePill();});
            layout=new WindowManager.LayoutParams(
                dp(148),WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT);
            layout.gravity=Gravity.TOP|Gravity.LEFT;
            updatePosition(false);
            windows.addView(pill,layout);
            attached=true;
            updatePill();
        }catch(Exception ex){
            attached=false;
            if(pill!=null&&windows!=null){
                try{windows.removeViewImmediate(pill);}catch(Exception ignored){}
            }
            pill=null;
        }
    }
    private TextView control(String s){
        TextView v=text(s,23,WHITE,true);v.setGravity(Gravity.CENTER);
        v.setBackground(shape(0xFF191C24,12));return v;
    }
    private void sendMedia(int cmd){
        MediaController c=controller;if(c==null)return;
        try{
            MediaController.TransportControls tc=c.getTransportControls();
            if(cmd==0)tc.skipToPrevious();
            else if(cmd==2)tc.skipToNext();
            else{
                PlaybackState ps=c.getPlaybackState();
                if(ps!=null&&ps.getState()==PlaybackState.STATE_PLAYING)tc.pause();
                else tc.play();
            }
        }catch(Exception ignored){}
        ui.postDelayed(this::refreshMedia,250);
    }
    private boolean mediaPlaying(){
        if(controller==null)return false;
        PlaybackState ps=controller.getPlaybackState();
        return ps!=null&&ps.getState()==PlaybackState.STATE_PLAYING;
    }
    private void transientMessage(String icon,String summary,String detail,int ms){
        if(prefs==null||!prefs.getBoolean("enabled",false))return;
        final int seq=++notificationSequence;
        ui.post(()->{
            ephemeralIcon=icon;ephemeralText=summary;ephemeralDetail=detail;
            expanded=true;ensurePill();updatePill();
        });
        ui.postDelayed(()->{
            if(notificationSequence!=seq)return;
            ephemeralIcon="";ephemeralText="";ephemeralDetail="";
            expanded=false;updatePill();
        },ms);
    }
    @Override public void onNotificationPosted(StatusBarNotification sbn){
        if(sbn==null||sbn.getNotification()==null||sbn.getPackageName().equals(getPackageName()))return;
        Notification n=sbn.getNotification();
        if(Notification.CATEGORY_TRANSPORT.equals(n.category)||Notification.CATEGORY_SERVICE.equals(n.category))return;
        if((n.flags & Notification.FLAG_ONGOING_EVENT)!=0)return;
        String pkg=sbn.getPackageName();
        String app=pkg;
        try{app=getPackageManager().getApplicationLabel(getPackageManager().getApplicationInfo(pkg,0)).toString();}
        catch(Exception ignored){}
        final String label=app;
        String title="",body="";
        if(n.extras!=null){
            CharSequence t=n.extras.getCharSequence(Notification.EXTRA_TITLE);
            CharSequence b=n.extras.getCharSequence(Notification.EXTRA_TEXT);
            if(t!=null)title=t.toString();
            if(b!=null)body=b.toString();
        }
        final String detailTitle=title,detailBody=body;
        ui.post(()->{
            currentApp=label;currentTitle=detailTitle;currentText=detailBody;
            boolean previews=prefs!=null&&prefs.getBoolean("preview_content",false);
            transientMessage("•",label,previews?(detailTitle+"  "+detailBody).trim():"Có thông báo mới",4800);
            refreshMedia();
        });
    }
    private void updatePill(){
        if(!attached||pill==null||compactIcon==null)return;
        boolean media=controller!=null&&controller.getMetadata()!=null;
        String label="Dynamic Pill",detail="Chạm để mở rộng",symbol="•";
        if(media){
            MediaMetadata m=controller.getMetadata();
            String song=m.getString(MediaMetadata.METADATA_KEY_TITLE);
            String artist=m.getString(MediaMetadata.METADATA_KEY_ARTIST);
            if(song!=null&&!song.trim().isEmpty())label=song;
            else label="Đang phát nhạc";
            detail=artist==null?"Trình phát nhạc":artist;
            symbol=mediaPlaying()?"♫":"Ⅱ";
        }else if(charging){
            symbol="ϟ";label="Sạc "+batteryPercent+"%";detail="Đang sạc pin";
        }
        if(!ephemeralText.isEmpty()){
            symbol=ephemeralIcon;label=ephemeralText;detail=ephemeralDetail;
        }
        compactIcon.setText(symbol);
        compactLabel.setText(label);
        titleView.setText(label);
        subtitleView.setText(detail);
        playButton.setText(mediaPlaying()?"⏸":"▶");
        controls.setVisibility(media?View.VISIBLE:View.GONE);
        details.setVisibility(expanded?View.VISIBLE:View.GONE);
        updatePosition(expanded);
        if(windows!=null){
            try{windows.updateViewLayout(pill,layout);}catch(Exception ignored){}
        }
    }
    private void updatePosition(boolean wide){
        if(layout==null||prefs==null)return;
        int w=prefs.getInt("pill_width_dp",148);
        w=Math.max(100,Math.min(250,w));
        int width=dp(wide?Math.max(312,w+150):w);
        int screen=getResources().getDisplayMetrics().widthPixels;
        int camera=prefs.getInt("camera_center_px",-1);
        if(camera<=0||camera>screen)camera=screen/2;
        layout.width=width;
        layout.gravity=Gravity.TOP|Gravity.LEFT;
        layout.x=Math.max(0,Math.min(screen-width,camera-width/2+
            dp(prefs.getInt("offset_x_dp",0))));
        layout.y=dp(7+prefs.getInt("offset_y_dp",0));
    }
    private void removePill(){
        ++notificationSequence;ephemeralIcon="";ephemeralText="";ephemeralDetail="";
        if(attached&&windows!=null&&pill!=null){
            try{windows.removeViewImmediate(pill);}catch(Exception ignored){}
        }
        attached=false;pill=null;compact=null;details=null;controls=null;
        compactIcon=null;compactLabel=null;titleView=null;subtitleView=null;playButton=null;
    }
    private void release(){
        removePill();
        if(sessionManager!=null&&sessionCallback!=null){
            try{sessionManager.removeOnActiveSessionsChangedListener(sessionCallback);}catch(Exception ignored){}
        }
        if(controller!=null&&controllerCallback!=null){
            try{controller.unregisterCallback(controllerCallback);}catch(Exception ignored){}
        }
        sessionManager=null;controller=null;sessionCallback=null;controllerCallback=null;
        if(listenersSet){
            try{unregisterReceiver(commands);}catch(Exception ignored){}
            try{unregisterReceiver(batteryReceiver);}catch(Exception ignored){}
            listenersSet=false;
        }
    }
}
