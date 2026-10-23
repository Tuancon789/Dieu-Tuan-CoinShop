package vn.dieutuan.dynamicpill;

import android.app.Activity;
import android.Manifest;
import android.os.Build;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.view.DisplayCutout;
import android.view.WindowInsets;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private static final int BACKGROUND=0xFF0D111A, CARD=0xFF1A2230;
    private static final int WHITE=0xFFF5F7FB, MUTED=0xFFA5B1C3, MINT=0xFF61E8D0;
    private LinearLayout content;
    private SharedPreferences prefs;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(BACKGROUND);
        getWindow().setNavigationBarColor(BACKGROUND);
        prefs=getSharedPreferences(IslandListenerService.PREFS,MODE_PRIVATE);
        draw();
        captureCameraPosition();
    }
    @Override protected void onResume() {
        super.onResume();
        draw();
        captureCameraPosition();
        if(prefs.getBoolean("enabled",false))startPersistentService();
    }
    private void captureCameraPosition(){
        getWindow().getDecorView().post(()->{
            try{
                if(Build.VERSION.SDK_INT<28)return;
                WindowInsets insets=getWindow().getDecorView().getRootWindowInsets();
                if(insets==null)return;
                DisplayCutout cutout=insets.getDisplayCutout();
                if(cutout==null)return;
                Rect best=null;
                for(Rect r:cutout.getBoundingRects()){
                    if(r.top<=d(35)&&(best==null||r.top<best.top))best=r;
                }
                if(best==null)return;
                int center=best.centerX();
                if(center>0&&center!=prefs.getInt("camera_center_px",-1)){
                    prefs.edit().putInt("camera_center_px",center).apply();
                    sendRefresh();
                }
            }catch(Exception ignored){}
        });
    }
    private void startPersistentService(){
        try{
            Intent i=new Intent(this,PillKeepAliveService.class);
            if(Build.VERSION.SDK_INT>=26)startForegroundService(i);
            else startService(i);
        }catch(Exception e){
            Toast.makeText(this,"Android đã chặn chạy nền; kiểm tra tiết kiệm pin",Toast.LENGTH_LONG).show();
        }
    }

    private int d(float dp){return (int)(dp*getResources().getDisplayMetrics().density+0.5f);}
    private GradientDrawable bg(int color,int radius){
        GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(d(radius));return g;
    }
    private TextView text(String label,int size,int color,boolean bold){
        TextView t=new TextView(this);t.setText(label);t.setTextSize(size);t.setTextColor(color);
        if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;
    }
    private LinearLayout column(){LinearLayout l=new LinearLayout(this);l.setOrientation(1);return l;}
    private void add(LinearLayout p,View v,int bottom){
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);
        lp.bottomMargin=d(bottom);p.addView(v,lp);
    }
    private LinearLayout card(){
        LinearLayout c=column();c.setPadding(d(18),d(17),d(18),d(17));c.setBackground(bg(CARD,19));return c;
    }
    private TextView button(String label, boolean strong, Runnable call){
        TextView b=text(label,15,strong?BACKGROUND:WHITE,true);
        b.setGravity(Gravity.CENTER);b.setPadding(d(12),d(14),d(12),d(14));
        b.setBackground(bg(strong?MINT:0xFF2B3647,13));b.setOnClickListener(v->call.run());return b;
    }
    private void draw(){
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setBackgroundColor(BACKGROUND);
        setContentView(scroll);
        content=column();content.setPadding(d(18),d(28),d(18),d(25));scroll.addView(content);
        TextView title=text("DYNAMIC PILL",25,WHITE,true);add(content,title,7);
        add(content,text("Viên thuốc đen • nhạc • thông báo • sạc pin",14,MUTED,false),22);

        LinearLayout preview=column();preview.setGravity(Gravity.CENTER);
        preview.setPadding(d(15),d(26),d(15),d(26));preview.setBackground(bg(0xFF171D28,20));
        TextView shape=text("♪    Nhạc đang phát       ▥",14,WHITE,true);
        shape.setGravity(Gravity.CENTER);shape.setBackground(bg(Color.BLACK,50));
        LinearLayout.LayoutParams previewLp=new LinearLayout.LayoutParams(d(236),d(48));
        previewLp.gravity=Gravity.CENTER_HORIZONTAL;preview.addView(shape,previewLp);
        add(preview,text("",5,MUTED,false),2);
        TextView caption=text("Chạm viên thuốc để mở rộng điều khiển",12,MUTED,false);
        caption.setGravity(Gravity.CENTER);preview.addView(caption);
        add(content,preview,15);

        boolean overlay=Settings.canDrawOverlays(this);
        boolean notifications=hasNotificationAccess();
        boolean enabled=prefs.getBoolean("enabled",false);
        LinearLayout permissions=card();
        add(permissions,text("QUYỀN CẦN THIẾT",13,MINT,true),12);
        add(permissions,text((overlay?"✓  ":"○  ")+"Hiển thị trên ứng dụng khác",16,WHITE,true),5);
        add(permissions,text("Giúp viên thuốc xuất hiện phía trên ứng dụng khác.",13,MUTED,false),10);
        if(!overlay)add(permissions,button("Cấp quyền cửa sổ nổi",true,()->{
            try{
                Intent intent=new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:"+getPackageName()));startActivity(intent);
            }catch(Exception e){Toast.makeText(this,"Không mở được cài đặt",Toast.LENGTH_SHORT).show();}
        }),15);
        add(permissions,text((notifications?"✓  ":"○  ")+"Truy cập thông báo / điều khiển nhạc",16,WHITE,true),5);
        add(permissions,text("Cần để đọc thông báo và tìm Media Session đang phát nhạc.",13,MUTED,false),10);
        if(!notifications)add(permissions,button("Cấp quyền thông báo",true,()->{
            try{startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));}
            catch(Exception e){Toast.makeText(this,"Không mở được cài đặt",Toast.LENGTH_SHORT).show();}
        }),2);
        add(content,permissions,15);

        LinearLayout settings=card();
        add(settings,text("TÙY CHỌN",13,MINT,true),12);
        boolean previews=prefs.getBoolean("preview_content",false);
        add(settings,text("Nội dung thông báo: "+(previews?"Hiện đoạn xem trước":"Ẩn (riêng tư)"),15,WHITE,true),6);
        add(settings,text("Mặc định chỉ hiện tên ứng dụng để tránh lộ tin nhắn.",13,MUTED,false),12);
        add(settings,button(previews?"Ẩn nội dung thông báo":"Cho phép hiện nội dung",false,()->{
            prefs.edit().putBoolean("preview_content",!previews).apply();sendRefresh();draw();
        }),13);
        add(settings,text("Viên thuốc sẽ hiển thị khi nhận thông báo, cắm sạc hoặc nghe nhạc.",13,MUTED,false),16);
        add(settings,text("VỊ TRÍ & KÍCH THƯỚC",13,MINT,true),9);
        add(settings,text(prefs.getInt("camera_center_px",-1)>0?
            "✓ Đã tìm thấy vị trí camera • có thể tinh chỉnh":
            "Chưa tìm được camera qua Android • dùng nút chỉnh tay",
            12,MUTED,false),8);
        add(settings,text("Trái / phải: "+prefs.getInt("offset_x_dp",0)+" dp • cao / thấp: "+
            prefs.getInt("offset_y_dp",0)+" dp",13,WHITE,false),8);
        add(settings,text("Chế độ ổn định: viên thuốc nằm ngay bên dưới thanh trạng thái để không bị các biểu tượng che. Vẫn căn ngang theo camera.",12,MUTED,false),9);
        LinearLayout horizontal=new LinearLayout(this);
        horizontal.setOrientation(0);
        settingButton(horizontal,"←",()->adjust("offset_x_dp",-10,-160,160));
        settingButton(horizontal,"→",()->adjust("offset_x_dp",10,-160,160));
        settingButton(horizontal,"↑",()->adjust("offset_y_dp",-10,-2,150));
        settingButton(horizontal,"↓",()->adjust("offset_y_dp",10,-2,150));
        add(settings,horizontal,9);
        add(settings,button("KHÔI PHỤC VỊ TRÍ & HIỆN VIÊN THUỐC",false,()->restorePill()),10);
        add(settings,text("Trạng thái: "+prefs.getString("pill_status","Chưa rõ")+" (lần gần nhất)",12,MUTED,false),12);
        add(settings,text("Độ rộng viên thuốc: "+prefs.getInt("pill_width_dp",148)+" dp",13,WHITE,false),7);
        LinearLayout widthButtons=new LinearLayout(this);
        widthButtons.setOrientation(0);
        settingButton(widthButtons,"− Hẹp",()->adjust("pill_width_dp",-10,100,250));
        settingButton(widthButtons,"+ Rộng",()->adjust("pill_width_dp",10,100,250));
        settingButton(widthButtons,"Đặt lại",()->{
            prefs.edit().putInt("offset_x_dp",0).putInt("offset_y_dp",0)
                .putInt("pill_width_dp",148).apply();
            sendRefresh();draw();
        });
        add(settings,widthButtons,14);
        add(settings,text("MÀN HÌNH KHÓA & CHẠY NỀN",13,MINT,true),7);
        add(settings,text("Khi bật: giữ dịch vụ nền bằng thông báo thường trực. Màn hình khóa có thể hiện thông báo này, nhưng Android có thể ẩn cửa sổ nổi.",12,MUTED,false),9);
        add(settings,button("Mở cài đặt pin (cho phép chạy nền)",false,()->{
            try{
                Intent i=new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:"+getPackageName()));startActivity(i);
            }catch(Exception e){Toast.makeText(this,"Không mở được cài đặt ứng dụng",Toast.LENGTH_SHORT).show();}
        }),8);
        if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                !=PackageManager.PERMISSION_GRANTED){
            add(settings,button("Cấp quyền thông báo trên màn hình khóa",false,()->
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},101)),5);
        }
        add(content,settings,15);

        if(enabled)add(content,button("HIỆN LẠI VIÊN THUỐC",false,()->restorePill()),10);
        add(content,button(enabled?"TẮT VIÊN THUỐC":"BẬT VIÊN THUỐC",true,()->{
            if(!enabled&&(!Settings.canDrawOverlays(this)||!hasNotificationAccess())){
                Toast.makeText(this,"Cấp đủ 2 quyền trước nhé",Toast.LENGTH_LONG).show();return;
            }
            prefs.edit().putBoolean("enabled",!enabled).apply();
            if(!enabled)startPersistentService();
            else stopService(new Intent(this,PillKeepAliveService.class));
            if(!enabled){
                try{android.service.notification.NotificationListenerService.requestRebind(
                    new ComponentName(this,IslandListenerService.class));}catch(Exception ignored){}
            }
            sendRefresh();draw();
        }),10);
        add(content,button("XEM THỬ HIỆU ỨNG (5 GIÂY)",false,()->{
            if(!prefs.getBoolean("enabled",false)){
                Toast.makeText(this,"Bật viên thuốc trước nhé",Toast.LENGTH_SHORT).show();return;
            }
            sendCommand(IslandListenerService.ACTION_PREVIEW);
        }),13);
        add(content,text("v1.3 ưu tiên không bị thanh trạng thái che. Android 14 có thể không cho ứng dụng thường đè lên cụm camera trước; vì vậy viên thuốc nằm sát phía dưới thanh trạng thái. Chạy nền phụ thuộc cài đặt pin của máy.",12,MUTED,false),15);
    }
    private void restorePill(){
        if(!Settings.canDrawOverlays(this)||!hasNotificationAccess()){
            Toast.makeText(this,"Cần cấp quyền cửa sổ nổi và truy cập thông báo",Toast.LENGTH_LONG).show();
            return;
        }
        prefs.edit().putBoolean("enabled",true).putInt("offset_x_dp",0)
            .putInt("offset_y_dp",0).putInt("pill_width_dp",148)
            .putString("pill_status","ĐANG KHÔI PHỤC").apply();
        startPersistentService();
        try{
            android.service.notification.NotificationListenerService.requestRebind(
                new ComponentName(this,IslandListenerService.class));
        }catch(Exception ignored){}
        sendCommand(IslandListenerService.ACTION_RECREATE);
        Toast.makeText(this,"Đang khôi phục viên thuốc",Toast.LENGTH_SHORT).show();
        draw();
    }
    private void settingButton(LinearLayout row,String label,Runnable command){
        TextView b=button(label,false,command);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,d(44),1);
        lp.setMargins(d(2),0,d(2),0);row.addView(b,lp);
    }
    private void adjust(String key,int delta,int lower,int upper){
        int updated=Math.max(lower,Math.min(upper,prefs.getInt(key,key.equals("pill_width_dp")?148:0)+delta));
        prefs.edit().putInt(key,updated).apply();
        sendRefresh();draw();
    }
    private boolean hasNotificationAccess(){
        NotificationManager nm=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        if(nm==null)return false;
        try{
            for(String s:Settings.Secure.getString(getContentResolver(),"enabled_notification_listeners").split(":")){
                ComponentName c=ComponentName.unflattenFromString(s);
                if(c!=null&&c.equals(new ComponentName(this,IslandListenerService.class)))return true;
            }
        }catch(Exception ignored){}
        return false;
    }
    private void sendRefresh(){sendCommand(IslandListenerService.ACTION_REFRESH);}
    private void sendCommand(String command){
        sendBroadcast(new Intent(command).setPackage(getPackageName()));
    }
}
