package vn.dieutuan.coinshop;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONObject;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class MainActivity extends Activity {
    private static final int BG=Color.rgb(247,248,251);
    private static final int PAPER=Color.WHITE, PALE=Color.rgb(241,243,247);
    private static final int INK=Color.rgb(43,57,75), MUTED=Color.rgb(106,119,136);
    private static final int PINK=Color.rgb(255,48,105), GOLD=Color.rgb(244,184,24);
    private static final int[] PACKS={30,100,200,500,1000};
    private static final long[] COSTS={9105,30400,60700,151800,303500};
    private SharedPreferences db;
    private LinearLayout outer, content, bottom;
    private boolean english;
    private String tab="shop", user="", recipient="";
    private boolean admin=false;
    private int selected=30;

    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        db=getSharedPreferences("coinshop_offline_v1",Context.MODE_PRIVATE);
        english=db.getBoolean("english",false);
        loadSession(); render();
    }
    private String tr(String vi,String en){return english?en:vi;}
    private int dp(float x){return (int)(x*getResources().getDisplayMetrics().density+0.5f);}
    private GradientDrawable shape(int color,int radius,int stroke){
        GradientDrawable d=new GradientDrawable();d.setColor(color);
        d.setCornerRadius(dp(radius));
        if(stroke!=0)d.setStroke(dp(1.6f),stroke);
        return d;
    }
    private void space(View v,int x,int y){v.setPadding(dp(x),dp(y),dp(x),dp(y));}
    private LinearLayout col(){LinearLayout l=new LinearLayout(this);l.setOrientation(1);return l;}
    private LinearLayout row(){LinearLayout l=new LinearLayout(this);l.setOrientation(0);l.setGravity(Gravity.CENTER_VERTICAL);return l;}
    private void add(LinearLayout parent,View item,int bottomMargin){
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);
        p.bottomMargin=dp(bottomMargin);parent.addView(item,p);
    }
    private TextView label(String s,int size,int color,boolean bold){
        TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);
        if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;
    }
    private TextView action(String s,boolean primary,Runnable tap){
        TextView t=label(s,15,primary?PAPER:INK,true);
        t.setGravity(Gravity.CENTER);space(t,12,15);
        t.setBackground(shape(primary?PINK:PALE,12,0));
        t.setOnClickListener(v->tap.run());return t;
    }
    private LinearLayout card(){
        LinearLayout l=col(); l.setBackground(shape(PAPER,18,0));space(l,16,16);return l;
    }
    private EditText field(String hint,boolean pass){
        EditText e=new EditText(this);e.setSingleLine();
        e.setTextSize(15);e.setTextColor(INK);e.setHintTextColor(MUTED);e.setHint(hint);
        e.setBackground(shape(PALE,11,0));space(e,12,12);
        e.setInputType(pass?InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD:InputType.TYPE_CLASS_TEXT);
        return e;
    }
    private ImageView coin(int diameter){
        ImageView i=new ImageView(this);i.setImageResource(R.drawable.coin_icon);
        i.setScaleType(ImageView.ScaleType.FIT_CENTER);
        i.setLayoutParams(new LinearLayout.LayoutParams(dp(diameter),dp(diameter)));
        return i;
    }
    private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_SHORT).show();}
    private String fmt(long n){return String.format(Locale.US,"%,d",n).replace(',','.')+"đ";}
    private long price(int n){
        for(int k=0;k<PACKS.length;k++)if(PACKS[k]==n)return COSTS[k];
        return Math.round(304.0*n);
    }
    private JSONArray users(){try{return new JSONArray(db.getString("users","[]"));}catch(Exception ex){return new JSONArray();}}
    private JSONObject find(String name){
        JSONArray a=users();
        for(int i=0;i<a.length();i++){
            JSONObject u=a.optJSONObject(i);
            if(u!=null&&u.optString("name").equalsIgnoreCase(name))return u;
        }return null;
    }
    private void loadSession(){
        JSONObject u=find(db.getString("session",""));
        user=u==null?"":u.optString("name");
        admin=u!=null&&u.optBoolean("admin",false);
        recipient=user.isEmpty()?"":db.getString("receiver_"+user.toLowerCase(Locale.US),"");
    }
    private String salt(){
        byte[] b=new byte[16];new SecureRandom().nextBytes(b);
        StringBuilder s=new StringBuilder();
        for(byte x:b)s.append(String.format(Locale.US,"%02x",x&255));return s.toString();
    }
    private String hash(String pwd,String hex)throws Exception{
        byte[] b=new byte[hex.length()/2];
        for(int i=0;i<b.length;i++)b[i]=(byte)Integer.parseInt(hex.substring(2*i,2*i+2),16);
        PBEKeySpec spec=new PBEKeySpec(pwd.toCharArray(),b,120000,256);
        try{
            byte[] dig=SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                .generateSecret(spec).getEncoded();
            StringBuilder out=new StringBuilder();
            for(byte x:dig)out.append(String.format(Locale.US,"%02x",x&255));
            return out.toString();
        }finally{spec.clearPassword();}
    }
    private void render(){
        outer=col();outer.setBackgroundColor(BG);setContentView(outer);
        LinearLayout head=row();head.setPadding(dp(18),dp(20),dp(18),dp(12));
        ImageView mark=coin(35);head.addView(mark);
        TextView title=label("  Coin Shop",22,INK,true);
        head.addView(title,new LinearLayout.LayoutParams(0,-2,1));
        TextView lang=label(english?"VI":"EN",14,PINK,true);
        lang.setGravity(Gravity.CENTER);space(lang,12,11);
        lang.setBackground(shape(PAPER,14,0));head.addView(lang);
        lang.setOnClickListener(v->{english=!english;db.edit().putBoolean("english",english).apply();render();});
        outer.addView(head);
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(false);
        outer.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        content=col();content.setPadding(dp(16),dp(8),dp(16),dp(20));scroll.addView(content);
        if(user.isEmpty()){login();return;}
        if(tab.equals("history"))history();
        else if(tab.equals("account"))account();
        else if(tab.equals("admin")&&admin)adminScreen();
        else shop();
        buildBottom();
    }
    private void buildBottom(){
        bottom=col();bottom.setBackground(shape(PAPER,18,0));space(bottom,12,10);
        LinearLayout nav=row();
        navItem(nav,tr("Nạp xu","Shop"),"shop");
        navItem(nav,tr("Đơn hàng","Orders"),"history");
        navItem(nav,tr("Tài khoản","Account"),"account");
        if(admin)navItem(nav,"ADMIN","admin");
        bottom.addView(nav);outer.addView(bottom);
    }
    private void navItem(LinearLayout nav,String name,String key){
        boolean active=tab.equals(key);
        TextView b=label(name,13,active?PINK:MUTED,active);
        b.setGravity(Gravity.CENTER);space(b,1,12);
        nav.addView(b,new LinearLayout.LayoutParams(0,-2,1));
        b.setOnClickListener(v->{tab=key;render();});
    }
    private void login(){
        LinearLayout c=card();
        add(c,label(tr("Chào mừng đến Coin Shop","Welcome to Coin Shop"),23,INK,true),8);
        add(c,label(tr("Đăng nhập để quản lý yêu cầu nạp xu.","Sign in to manage your coin requests."),14,MUTED,false),20);
        EditText name=field(tr("Tên tài khoản","Username"),false);
        EditText pass=field(tr("Mật khẩu","Password"),true);
        add(c,name,10);add(c,pass,14);
        add(c,action(tr("Đăng nhập","Sign in"),true,()->{
            JSONObject u=find(name.getText().toString().trim());
            if(u==null){toast(tr("Không tìm thấy tài khoản","Account not found"));return;}
            try{
                byte[] a=u.optString("hash").getBytes("UTF-8");
                byte[] b=hash(pass.getText().toString(),u.optString("salt")).getBytes("UTF-8");
                if(!MessageDigest.isEqual(a,b)){toast(tr("Sai mật khẩu","Wrong password"));return;}
                db.edit().putString("session",u.optString("name")).apply();
                loadSession();tab="shop";render();
            }catch(Exception e){toast(tr("Không đăng nhập được","Could not sign in"));}
        }),10);
        add(c,action(tr("Tạo tài khoản","Create account"),false,()->{
            String n=name.getText().toString().trim(),p=pass.getText().toString();
            if(!n.matches("[A-Za-z0-9._]{3,24}")||p.length()<8){
                toast(tr("Tên 3–24 ký tự, mật khẩu ít nhất 8 ký tự","Username 3–24 chars, password 8+ chars"));return;
            }
            if(find(n)!=null){toast(tr("Tên đã tồn tại","Username exists"));return;}
            try{
                JSONArray a=users();JSONObject u=new JSONObject();
                u.put("name",n);u.put("admin",a.length()==0);
                String s=salt();u.put("salt",s);u.put("hash",hash(p,s));a.put(u);
                db.edit().putString("users",a.toString()).putString("session",n).apply();
                loadSession();tab="shop";render();
            }catch(Exception e){toast(tr("Không tạo được tài khoản","Cannot create account"));}
        }),12);
        add(c,label(tr("Tài khoản hiện được lưu trên điện thoại này, chưa đồng bộ máy chủ.",
            "Accounts are stored on this device; server sync is not available."),12,MUTED,false),0);
        add(content,c,14);
    }
    private void shop(){
        LinearLayout identity=card();
        add(identity,label(tr("TÀI KHOẢN NHẬN XU","COIN RECIPIENT"),12,MUTED,true),10);
        LinearLayout userRow=row();
        TextView at=label("@",24,PINK,true);at.setGravity(Gravity.CENTER);
        at.setBackground(shape(PALE,26,0));
        LinearLayout.LayoutParams pic=new LinearLayout.LayoutParams(dp(46),dp(46));
        pic.rightMargin=dp(12);userRow.addView(at,pic);
        LinearLayout names=col();
        add(names,label(recipient.isEmpty()?tr("Chưa chọn ID TikTok","No TikTok ID saved"):"@"+recipient,17,INK,true),4);
        add(names,label(tr("ID sẽ áp dụng cho đơn tiếp theo","Used for your next order"),12,MUTED,false),0);
        userRow.addView(names,new LinearLayout.LayoutParams(0,-2,1));
        TextView change=label(tr("Đổi ID ›","Edit ›"),14,PINK,true);
        change.setOnClickListener(v->editRecipient());
        userRow.addView(change);
        add(identity,userRow,0);
        add(content,identity,14);
        LinearLayout packs=card();
        add(packs,label(tr("Nạp xu","Coin packages"),20,INK,true),4);
        add(packs,label(tr("Chọn số lượng xu phù hợp","Choose a coin quantity"),13,MUTED,false),17);
        for(int i=0;i<PACKS.length;i+=3){
            LinearLayout r=row();r.setGravity(Gravity.TOP);
            for(int j=i;j<Math.min(i+3,PACKS.length);j++)addPackage(r,PACKS[j]);
            if(i==3)addCustom(r);
            add(packs,r,9);
        }
        add(content,packs,14);
        LinearLayout pay=card();
        add(pay,label(tr("Phương thức thanh toán","Payment method"),17,INK,true),13);
        LinearLayout pp=row();
        TextView logo=label("P",19,Color.WHITE,true);logo.setGravity(Gravity.CENTER);
        logo.setBackground(shape(Color.rgb(24,58,133),10,0));
        pp.addView(logo,new LinearLayout.LayoutParams(dp(40),dp(40)));
        TextView ppLabel=label("  PayPal",16,INK,true);
        pp.addView(ppLabel,new LinearLayout.LayoutParams(0,-2,1));
        pp.addView(label("✓",22,PINK,true));
        add(pay,pp,9);
        add(pay,label(tr("Chưa kết nối xử lý thanh toán PayPal trong ứng dụng.",
            "PayPal processing is not yet connected in the app."),12,MUTED,false),0);
        add(content,pay,10);
        add(content,label(tr("Giá hiển thị để tham khảo trước khi đặt yêu cầu. Đơn hiện chỉ lưu trên thiết bị.",
            "Shown prices are estimates; requests are currently saved on this device only."),12,MUTED,false),16);
        add(content,action(tr("GỬI YÊU CẦU • ","SUBMIT REQUEST • ")+selected+" xu  —  "+fmt(price(selected)),
            true,()->confirm()),12);
    }
    private void addPackage(LinearLayout r,int n){
        LinearLayout tile=col();tile.setGravity(Gravity.CENTER);
        tile.setBackground(shape(selected==n?Color.rgb(255,243,247):PALE,13,selected==n?PINK:0));
        space(tile,3,14);
        ImageView i=coin(30);tile.addView(i);
        TextView amount=label(String.format(Locale.US,"%,d",n),17,INK,true);
        amount.setGravity(Gravity.CENTER);add(tile,amount,4);
        TextView cost=label(fmt(price(n)),12,MUTED,false);
        cost.setGravity(Gravity.CENTER);add(tile,cost,0);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(112),1);
        lp.rightMargin=dp(7);r.addView(tile,lp);
        tile.setOnClickListener(v->{selected=n;render();});
    }
    private void addCustom(LinearLayout r){
        LinearLayout tile=col();tile.setGravity(Gravity.CENTER);
        tile.setBackground(shape(PALE,13,0));space(tile,4,12);
        TextView t=label(tr("Tùy chỉnh","Custom"),16,INK,true);t.setGravity(Gravity.CENTER);
        tile.addView(t);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(112),1);
        lp.rightMargin=dp(7);r.addView(tile,lp);
        tile.setOnClickListener(v->custom());
    }
    private void custom(){
        EditText e=field(tr("Số xu (1 – 2.500.000)","Coins (1 – 2,500,000)"),false);
        e.setInputType(InputType.TYPE_CLASS_NUMBER);
        e.setText(String.valueOf(selected));e.setSelection(e.getText().length());
        LinearLayout w=col();space(w,18,12);add(w,e,0);
        new AlertDialog.Builder(this)
            .setTitle(tr("Tùy chỉnh số lượng xu","Custom quantity"))
            .setView(w)
            .setNegativeButton(tr("Hủy","Cancel"),(d,x)->{})
            .setPositiveButton(tr("Áp dụng","Apply"),(d,x)->{
                try{
                    int n=Integer.parseInt(e.getText().toString());
                    if(n<1||n>2500000){toast(tr("Số lượng không hợp lệ","Invalid quantity"));return;}
                    selected=n;render();
                }catch(Exception ex){toast(tr("Hãy nhập số xu","Enter a quantity"));}
            }).show();
    }
    private void editRecipient(){
        EditText edit=field(tr("ID TikTok của khách","Customer TikTok ID"),false);
        edit.setText(recipient);edit.setSelection(edit.getText().length());
        LinearLayout w=col();space(w,18,14);add(w,edit,0);
        new AlertDialog.Builder(this)
            .setTitle(tr("Đổi ID TikTok nhận xu","Change recipient TikTok ID"))
            .setView(w)
            .setNegativeButton(tr("Hủy","Cancel"),(d,x)->{})
            .setPositiveButton(tr("Lưu ID","Save ID"),(d,x)->{
                String value=edit.getText().toString().trim().replaceFirst("^@","");
                if(!value.matches("[A-Za-z0-9._]{2,32}")){
                    toast(tr("ID TikTok không hợp lệ","Invalid TikTok ID"));return;
                }
                recipient=value;
                db.edit().putString("receiver_"+user.toLowerCase(Locale.US),recipient).apply();
                render();toast(tr("Đã lưu ID nhận xu","Recipient ID saved"));
            }).show();
    }
    private JSONArray orders(){
        try{return new JSONArray(db.getString("orders","[]"));}catch(Exception e){return new JSONArray();}
    }
    private void confirm(){
        if(recipient.isEmpty()){editRecipient();return;}
        new AlertDialog.Builder(this)
            .setTitle(tr("Xác nhận yêu cầu","Confirm request"))
            .setMessage("@"+recipient+"\n"+selected+" xu  •  "+fmt(price(selected))+
                "\n\n"+tr("Yêu cầu sẽ lưu trên máy và chưa phải xác nhận thanh toán hoặc giao xu.",
                "The request will be saved locally, not as proof of payment or delivery."))
            .setNegativeButton(tr("Hủy","Cancel"),(d,w)->{})
            .setPositiveButton(tr("Gửi yêu cầu","Submit request"),(d,w)->saveOrder()).show();
    }
    private void saveOrder(){
        try{
            JSONArray a=orders();JSONObject o=new JSONObject();
            String id="REQ-"+Long.toHexString(System.currentTimeMillis()).toUpperCase(Locale.US);
            o.put("id",id);o.put("buyer",user);o.put("recipient",recipient);
            o.put("coins",selected);o.put("vnd",price(selected));
            o.put("status","PENDING");o.put("time",System.currentTimeMillis());
            a.put(o);db.edit().putString("orders",a.toString()).apply();
            new AlertDialog.Builder(this)
                .setTitle(tr("Đã lưu yêu cầu","Request saved"))
                .setMessage(id+"\n"+tr("Đang chờ xác nhận. Chưa xác nhận thanh toán hoặc chuyển xu.",
                    "Awaiting confirmation. Payment and coin delivery are not confirmed."))
                .setPositiveButton(tr("Xem đơn","View orders"),(d,w)->{tab="history";render();})
                .show();
        }catch(Exception e){toast(tr("Không thể lưu yêu cầu","Could not save request"));}
    }
    private void history(){
        add(content,label(tr("Đơn hàng của tôi","My requests"),23,INK,true),16);
        showOrders(false);
    }
    private void showOrders(boolean all){
        JSONArray a=orders();int count=0;
        for(int i=a.length()-1;i>=0;i--){
            JSONObject o=a.optJSONObject(i);
            if(o==null||(!all&&!user.equalsIgnoreCase(o.optString("buyer"))))continue;
            count++;
            LinearLayout c=card();
            add(c,label("🪙 "+o.optInt("coins")+" xu  •  "+fmt(o.optLong("vnd")),17,INK,true),7);
            add(c,label(o.optString("id")+"  •  @"+o.optString("recipient"),13,MUTED,false),7);
            String status="SIMULATED".equals(o.optString("status"))?
                tr("Đơn cũ • chỉ mô phỏng","Old test order"):
                tr("Đang chờ xác nhận","Awaiting confirmation");
            add(c,label(status,13,PINK,true),7);
            add(c,label(new SimpleDateFormat("dd/MM/yyyy HH:mm",Locale.getDefault())
                .format(new Date(o.optLong("time"))),12,MUTED,false),0);
            if(all)add(c,label(tr("Tài khoản đặt: ","Placed by: ")+o.optString("buyer"),12,MUTED,false),0);
            add(content,c,10);
        }
        if(count==0)add(content,label(tr("Chưa có yêu cầu nào","No requests yet"),15,MUTED,false),10);
    }
    private void account(){
        add(content,label(tr("Tài khoản","Account"),23,INK,true),16);
        LinearLayout c=card();
        add(c,label(user,19,INK,true),6);
        add(c,label(admin?"ADMIN":tr("Khách hàng","Customer"),13,MUTED,false),15);
        add(c,action(tr("Thay đổi ID TikTok","Change TikTok ID"),false,()->editRecipient()),10);
        add(c,action(tr("Đăng xuất","Sign out"),true,()->{
            db.edit().remove("session").apply();loadSession();tab="shop";render();
        }),14);
        add(c,label(tr("Tài khoản và đơn đang lưu cục bộ. Không dùng trên nhiều thiết bị.",
           "Accounts and requests are stored locally, without cross-device sync."),12,MUTED,false),0);
        add(content,c,10);
    }
    private void adminScreen(){
        add(content,label("ADMIN",23,INK,true),14);
        LinearLayout c=card();
        add(c,label(tr("Số dư mẫu","Sample balance"),13,MUTED,true),7);
        add(c,label("$10,000,000.00",26,INK,true),10);
        add(c,label(tr("Đây là số liệu mẫu, không phải tiền trong PayPal.",
            "Illustrative amount, not a PayPal balance."),12,MUTED,false),17);
        add(c,label(tr("Tổng số yêu cầu trên máy: ","Local requests: ")+orders().length(),15,INK,true),0);
        add(content,c,18);
        add(content,label(tr("Đơn hàng trên thiết bị","Orders on this device"),19,INK,true),12);
        showOrders(true);
    }
}
