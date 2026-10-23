package vn.dieutuan.coinshop;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
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
    private static final int BG=Color.rgb(12,19,33), CARD=Color.rgb(26,38,59);
    private static final int PINK=Color.rgb(255,46,111), MINT=Color.rgb(51,226,196);
    private static final int WHITE=Color.WHITE, MUTED=Color.rgb(173,188,209);
    private static final int[] PACKS={30,100,200,500,1000};
    private static final long[] COSTS={9105,30400,60700,151800,303500};
    private static final String DEMO="SIMULATED";
    private SharedPreferences db;
    private LinearLayout root, body;
    private boolean english;
    private String tab="shop", draftRecipient="";
    private int selected=30;
    private String currentUser="";
    private boolean currentAdmin=false;

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        db=getSharedPreferences("coinshop_offline_v1", Context.MODE_PRIVATE);
        english=db.getBoolean("english",false);
        loadSession();
        render();
    }
    private String tr(String vi,String en){return english?en:vi;}
    private int dp(int x){return (int)(x*getResources().getDisplayMetrics().density+0.5f);}
    private GradientDrawable shape(int color,int radius){
        GradientDrawable d=new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(radius)); return d;
    }
    private void pad(View v,int a,int b,int c,int d){v.setPadding(dp(a),dp(b),dp(c),dp(d));}
    private LinearLayout column(){LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);return c;}
    private LinearLayout row(){LinearLayout r=new LinearLayout(this);r.setOrientation(LinearLayout.HORIZONTAL);r.setGravity(android.view.Gravity.CENTER_VERTICAL);return r;}
    private void margin(LinearLayout parent, View child, int bottom){
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);
        lp.bottomMargin=dp(bottom);parent.addView(child,lp);
    }
    private TextView text(String s,int sp,int color, boolean bold){
        TextView t=new TextView(this);t.setText(s);t.setTextColor(color);t.setTextSize(sp);
        if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;
    }
    private TextView button(String s,boolean accent,Runnable click){
        TextView b=text(s,15,WHITE,true);b.setGravity(android.view.Gravity.CENTER);
        pad(b,12,14,12,14);b.setBackground(shape(accent?PINK:CARD,12));b.setOnClickListener(v->click.run());return b;
    }
    private EditText field(String hint,boolean password){
        EditText e=new EditText(this);e.setSingleLine(true);
        e.setTextColor(WHITE);e.setHintTextColor(MUTED);e.setTextSize(15);e.setHint(hint);
        e.setInputType(password?InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD:InputType.TYPE_CLASS_TEXT);
        e.setBackground(shape(CARD,12));pad(e,13,12,13,12);return e;
    }
    private LinearLayout card(){LinearLayout c=column();c.setBackground(shape(CARD,16));pad(c,16,16,16,16);return c;}
    private void note(LinearLayout parent){
        TextView t=text(tr("MÔ PHỎNG • Không thanh toán hay nạp xu TikTok thật","DEMO ONLY • No real payment or TikTok coins"),12,MINT,true);
        t.setGravity(android.view.Gravity.CENTER);t.setBackground(shape(Color.rgb(14,67,66),10));pad(t,10,10,10,10);margin(parent,t,16);
    }
    private void render(){
        root=column();root.setBackgroundColor(BG);setContentView(root);
        ScrollView sc=new ScrollView(this);root.addView(sc,new LinearLayout.LayoutParams(-1,-1));
        body=column();pad(body,18,22,18,28);sc.addView(body);
        LinearLayout top=row();
        TextView title=text("ĐIỂU TUẤN  COIN SHOP",20,WHITE,true);
        top.addView(title,new LinearLayout.LayoutParams(0,-2,1));
        TextView lang=button(english?"VI":"EN",false,()->{
            english=!english;db.edit().putBoolean("english",english).apply();render();
        });top.addView(lang);margin(body,top,16);
        note(body);
        if(currentUser.isEmpty()){loginScreen();return;}
        TextView who=text(tr("Xin chào, ","Welcome, ")+currentUser+(currentAdmin?"  ★ ADMIN":""),16,WHITE,true);
        margin(body,who,14);
        LinearLayout nav=row();
        navButton(nav,tr("Nạp xu","Shop"),"shop");
        navButton(nav,tr("Lịch sử","History"),"history");
        navButton(nav,tr("Tài khoản","Account"),"account");
        if(currentAdmin)navButton(nav,"ADMIN","admin");
        margin(body,nav,20);
        if(tab.equals("history"))historyScreen();
        else if(tab.equals("account"))accountScreen();
        else if(tab.equals("admin")&&currentAdmin)adminScreen();
        else shopScreen();
    }
    private void navButton(LinearLayout bar,String title,String value){
        TextView b=text(title,12,tab.equals(value)?MINT:WHITE,tab.equals(value));
        b.setGravity(android.view.Gravity.CENTER);b.setBackground(shape(CARD,8));pad(b,3,11,3,11);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-2,1);
        lp.rightMargin=dp(5);bar.addView(b,lp);
        b.setOnClickListener(v->{tab=value;render();});
    }
    private JSONArray users(){
        try{return new JSONArray(db.getString("users","[]"));}catch(Exception ex){return new JSONArray();}
    }
    private JSONObject find(String name){
        JSONArray us=users();
        for(int i=0;i<us.length();i++){
            JSONObject u=us.optJSONObject(i);
            if(u!=null&&u.optString("name").equalsIgnoreCase(name))return u;
        }return null;
    }
    private void loadSession(){
        String s=db.getString("session","");
        JSONObject found=find(s);
        currentUser=found==null?"":found.optString("name","");
        currentAdmin=found!=null&&found.optBoolean("admin",false);
    }
    private String encode(String password,String saltHex)throws Exception{
        byte[] salt=new byte[saltHex.length()/2];
        for(int i=0;i<salt.length;i++)salt[i]=(byte)Integer.parseInt(saltHex.substring(i*2,i*2+2),16);
        PBEKeySpec spec=new PBEKeySpec(password.toCharArray(),salt,120000,256);
        try{
            byte[] digest=SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
            StringBuilder out=new StringBuilder();
            for(byte b:digest)out.append(String.format(Locale.US,"%02x",b&255));
            return out.toString();
        }finally{spec.clearPassword();}
    }
    private String newSalt(){
        byte[] b=new byte[16];new SecureRandom().nextBytes(b);
        StringBuilder s=new StringBuilder();for(byte x:b)s.append(String.format(Locale.US,"%02x",x&255));return s.toString();
    }
    private void loginScreen(){
        TextView heading=text(tr("Đăng nhập / Tạo tài khoản","Sign in / Create account"),24,WHITE,true);
        margin(body,heading,12);
        margin(body,text(tr("Bản thử nghiệm chỉ lưu tài khoản trên thiết bị này.",
            "Test accounts are stored on this device only."),13,MUTED,false),20);
        EditText name=field(tr("Tên đăng nhập (3–24 ký tự)","Username (3–24 characters)"),false);
        EditText pass=field(tr("Mật khẩu (ít nhất 8 ký tự)","Password (at least 8 characters)"),true);
        margin(body,name,10);margin(body,pass,14);
        margin(body,button(tr("Đăng nhập","Sign in"),true,()->{
            JSONObject u=find(name.getText().toString().trim());
            if(u==null){toast(tr("Không tìm thấy tài khoản","Account not found"));return;}
            try{
                String expected=u.optString("hash");
                if(!MessageDigest.isEqual(expected.getBytes("UTF-8"),
                    encode(pass.getText().toString(),u.optString("salt")).getBytes("UTF-8"))){
                    toast(tr("Sai mật khẩu","Wrong password"));return;
                }
                db.edit().putString("session",u.optString("name")).apply();loadSession();tab="shop";render();
            }catch(Exception ex){toast(tr("Không thể đăng nhập","Sign in failed"));}
        }),12);
        margin(body,button(tr("Tạo tài khoản mới","Create account"),false,()->{
            String n=name.getText().toString().trim(),p=pass.getText().toString();
            if(!n.matches("[A-Za-z0-9._]{3,24}")){toast(tr("Tên tài khoản không hợp lệ","Invalid username"));return;}
            if(p.length()<8){toast(tr("Mật khẩu quá ngắn","Password is too short"));return;}
            if(find(n)!=null){toast(tr("Tài khoản đã tồn tại","Account already exists"));return;}
            try{
                JSONArray a=users();JSONObject u=new JSONObject();
                boolean admin=a.length()==0;
                u.put("name",n);u.put("admin",admin);
                String salt=newSalt();u.put("salt",salt);u.put("hash",encode(p,salt));a.put(u);
                db.edit().putString("users",a.toString()).putString("session",n).apply();
                loadSession();tab="shop";render();
                toast(admin?tr("Đã tạo ADMIN thử nghiệm trên máy này","Local demo ADMIN created"):
                    tr("Tạo tài khoản thành công","Account created"));
            }catch(Exception ex){toast(tr("Không thể tạo tài khoản","Cannot create account"));}
        }),10);
        margin(body,text(tr("Tài khoản đầu tiên trên từng điện thoại là ADMIN DEMO, không phải quyền quản trị máy chủ.",
            "The first account on each device is a DEMO ADMIN, not server-level access."),12,MUTED,false),0);
    }
    private long price(int n){
        for(int i=0;i<PACKS.length;i++)if(PACKS[i]==n)return COSTS[i];
        return Math.round(n*304.0);
    }
    private String money(long n){return String.format(Locale.US,"%,d đ",n);}
    private void shopScreen(){
        margin(body,text(tr("Chọn gói xu","Select a coin package"),22,WHITE,true),12);
        LinearLayout box=card();
        margin(box,text(tr("Tài khoản TikTok nhận xu (chỉ ghi chú)", "Recipient TikTok handle (note only)"),13,MUTED,false),10);
        EditText recipient=field("@username",false);recipient.setText(draftRecipient);
        margin(box,recipient,16);
        for(int i=0;i<PACKS.length;i+=2){
            LinearLayout r=row();
            for(int j=i;j<Math.min(i+2,PACKS.length);j++){
                final int coins=PACKS[j];
                String caption="🪙 "+coins+"\n"+money(price(coins));
                TextView item=button(caption,selected==coins,()->{
                    draftRecipient=recipient.getText().toString().trim();selected=coins;render();
                });
                item.setMinHeight(dp(90));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-2,1);
                if(j%2==0)lp.rightMargin=dp(8);r.addView(item,lp);
            }margin(box,r,8);
        }
        EditText custom=field(tr("Số xu tùy chỉnh","Custom coin quantity"),false);
        custom.setInputType(InputType.TYPE_CLASS_NUMBER);
        margin(box,custom,8);
        margin(box,button(tr("Áp dụng số xu tùy chỉnh","Apply custom quantity"),false,()->{
            try{
                int c=Integer.parseInt(custom.getText().toString());
                if(c<1||c>100000){toast(tr("Nhập từ 1 đến 100.000 xu","Choose 1 to 100,000 coins"));return;}
                selected=c;draftRecipient=recipient.getText().toString().trim();render();
            }catch(Exception e){toast(tr("Nhập số xu hợp lệ","Enter valid coin quantity"));}
        }),14);
        margin(box,text(tr("Đang chọn: ","Selected: ")+selected+" 🪙  •  "+money(price(selected)),17,MINT,true),12);
        margin(box,text("PayPal  •  "+tr("Chỉ là nhãn phương thức, không kết nối PayPal","Display only, no PayPal connection"),13,MUTED,false),12);
        margin(box,button(tr("TẠO ĐƠN THỬ NGHIỆM","PLACE TEST ORDER"),true,()->{
            draftRecipient=recipient.getText().toString().trim();
            if(!draftRecipient.matches("@?[A-Za-z0-9._]{2,32}")){
                toast(tr("Nhập tài khoản nhận hợp lệ","Enter a valid recipient handle"));return;
            }
            new AlertDialog.Builder(this)
                .setTitle(tr("Xác nhận đơn mô phỏng","Confirm simulated order"))
                .setMessage(selected+" xu • "+money(price(selected))+
                    "\n"+tr("Không trừ tiền PayPal và không chuyển xu TikTok.","No PayPal payment or TikTok coin transfer."))
                .setNegativeButton(tr("Hủy","Cancel"),(d,w)->{})
                .setPositiveButton(tr("Tạo đơn","Create test order"),(d,w)->createOrder()).show();
        }),8);
        margin(box,text(tr("Không dùng bản demo này để xác nhận khách đã thanh toán hoặc đã nhận xu.",
            "Do not use this demo as proof of payment or coin delivery."),12,MUTED,false),0);
        margin(body,box,16);
    }
    private JSONArray orders(){
        try{return new JSONArray(db.getString("orders","[]"));}catch(Exception e){return new JSONArray();}
    }
    private void createOrder(){
        try{
            JSONArray a=orders();JSONObject o=new JSONObject();
            o.put("id","TEST-"+Long.toHexString(System.currentTimeMillis()).toUpperCase(Locale.US));
            o.put("buyer",currentUser);o.put("recipient",draftRecipient);
            o.put("coins",selected);o.put("vnd",price(selected));
            o.put("status",DEMO);o.put("time",System.currentTimeMillis());
            a.put(o);db.edit().putString("orders",a.toString()).apply();
            new AlertDialog.Builder(this)
                .setTitle(tr("Tạo đơn mô phỏng thành công!","Test order created!"))
                .setMessage(tr("Đơn đã lưu trên thiết bị. Không có thanh toán PayPal hoặc nạp xu thực tế.",
                  "Order saved on this device. No real PayPal payment or coin delivery.")+"\n"+o.optString("id"))
                .setPositiveButton("OK",(d,w)->{tab="history";render();}).show();
        }catch(Exception ex){toast(tr("Lưu đơn thất bại","Could not save order"));}
    }
    private void historyScreen(){
        margin(body,text(tr("Lịch sử đơn mô phỏng","Simulated order history"),22,WHITE,true),16);
        showOrders(false);
    }
    private void showOrders(boolean all){
        JSONArray a=orders();int count=0;
        for(int i=a.length()-1;i>=0;i--){
            JSONObject o=a.optJSONObject(i);if(o==null)continue;
            if(!all&&!currentUser.equalsIgnoreCase(o.optString("buyer")))continue;
            count++;
            LinearLayout c=card();
            margin(c,text("🪙 "+o.optInt("coins")+" xu  •  "+money(o.optLong("vnd")),18,WHITE,true),5);
            margin(c,text(o.optString("id")+"  |  @"+o.optString("recipient").replaceFirst("^@",""),12,MUTED,false),6);
            margin(c,text(tr("Trạng thái: THỬ NGHIỆM","Status: SIMULATED")+"  •  "+
                new SimpleDateFormat("dd/MM/yyyy HH:mm",Locale.getDefault()).format(new Date(o.optLong("time"))),12,MINT,true),0);
            if(all)margin(c,text(tr("Khách: ","Buyer: ")+o.optString("buyer"),12,MUTED,false),0);
            margin(body,c,10);
        }
        if(count==0)margin(body,text(tr("Chưa có đơn nào","No orders yet"),14,MUTED,false),8);
    }
    private void accountScreen(){
        margin(body,text(tr("Tài khoản","Account"),22,WHITE,true),15);
        LinearLayout c=card();
        margin(c,text(currentUser,20,WHITE,true),10);
        margin(c,text(currentAdmin?"ADMIN DEMO":tr("Khách thử nghiệm","Demo customer"),14,MINT,true),12);
        margin(c,button(tr("Đăng xuất","Sign out"),true,()->{
            db.edit().remove("session").apply();loadSession();tab="shop";render();
        }),0);margin(body,c,12);
        margin(body,text(tr("Tài khoản, đơn và lịch sử chỉ lưu trên điện thoại này.",
            "Accounts and orders stay on this device."),12,MUTED,false),0);
    }
    private void adminScreen(){
        margin(body,text("ADMIN  •  "+tr("Bảng điều khiển thử nghiệm","Demo dashboard"),21,WHITE,true),14);
        LinearLayout c=card();
        margin(c,text(tr("SỐ DƯ MÔ PHỎNG","SIMULATED BALANCE"),13,MUTED,true),5);
        margin(c,text("$10,000,000.00",28,MINT,true),10);
        margin(c,text(tr("Không phải tiền thật hoặc tài khoản PayPal.","Not real money or a PayPal account."),12,MUTED,false),12);
        margin(c,text(tr("Tổng đơn thử nghiệm: ","Total test orders: ")+orders().length(),16,WHITE,true),0);
        margin(body,c,18);
        margin(body,text(tr("Danh sách đơn trên máy","Orders on this device"),18,WHITE,true),12);
        showOrders(true);
    }
    private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_SHORT).show();}
}
