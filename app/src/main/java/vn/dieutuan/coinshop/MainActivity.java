package vn.dieutuan.coinshop;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import java.math.BigDecimal;
import java.math.RoundingMode;
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
    // Fixed reference conversion for display, not an exchange/payment service.
    private static final long VND_PER_USD=25850L;
    private static final int[] PACKS={30,100,200,500,1000};
    private static final long[] COSTS={9105,30400,60700,151800,303500};
    private SharedPreferences db;
    private LinearLayout outer, content, bottom;
    private boolean english;
    private String tab="shop", user="", recipient="";
    private boolean admin=false;
    private int selected=30;
    private int lastCoins=0;
    private long lastCost=0L;
    private String lastRecipient="",lastOrder="";

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
    private String fmt(long n){
        if(english)return "≈"+String.format(Locale.US,"$%,.2f",n/(double)VND_PER_USD);
        return String.format(Locale.US,"%,d",n).replace(',','.')+"đ";
    }
    private String rateHint(){return tr("Số dư lưu bằng VND.","USD quy đổi tham khảo • 1 USD = 25.850 VND (23/10/2026) • ví lưu bằng VND");}
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
        if(tab.equals("success"))successScreen();
        else if(tab.equals("history"))history();
        else if(tab.equals("account"))account();
        else if(tab.equals("admin")&&admin)adminScreen();
        else shop();
        if(!tab.equals("success"))buildBottom();
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
        boolean first=users().length()==0;
        add(c,label(first?tr("Thiết lập tài khoản ADM","Set up ADM account"):
            tr("Đăng nhập Coin Shop","Sign in to Coin Shop"),23,INK,true),8);
        add(c,label(first?
            tr("Lần đầu: tạo tài khoản quản trị ADM và tự đặt mật khẩu trên máy.",
               "First launch: create local ADM account and choose a password."):
            tr("Ví nội bộ đang lưu trên thiết bị này.",
               "Local wallet data is stored on this device."),13,MUTED,false),18);
        EditText name=field(tr("Tên tài khoản","Username"),false);
        if(first)name.setText("ADM");
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
        add(c,action(first?tr("Tạo ADMIN ADM","Create ADM admin"):
            tr("Tạo tài khoản","Create account"),false,()->{
            String n=name.getText().toString().trim(),p=pass.getText().toString();
            if(!n.matches("[A-Za-z0-9._]{3,24}")||p.length()<8){
                toast(tr("Tên 3–24 ký tự; mật khẩu ít nhất 8 ký tự",
                    "Username 3–24 chars; password 8+ chars"));return;
            }
            if(find(n)!=null){toast(tr("Tên đã tồn tại","Username exists"));return;}
            JSONArray a=users();
            if(a.length()==0&&!n.equalsIgnoreCase("ADM")){
                toast(tr("Tài khoản đầu tiên phải là ADM","First account must be ADM"));return;
            }
            if(a.length()!=0&&n.equalsIgnoreCase("ADM")){
                toast(tr("ADM chỉ do quản trị viên tạo","ADM must be created by an admin"));return;
            }
            try{
                JSONObject u=new JSONObject();
                u.put("name",n);u.put("admin",a.length()==0);
                String s=salt();u.put("salt",s);u.put("hash",hash(p,s));a.put(u);
                db.edit().putString("users",a.toString()).putString("session",n).apply();
                loadSession();tab="shop";render();
            }catch(Exception e){toast(tr("Không tạo được tài khoản","Cannot create account"));}
        }),12);
        add(c,label(tr("ADMIN chỉ quản lý tài khoản trên cùng điện thoại. Bản này chưa có máy chủ.",
           "ADM manages accounts on the same device only. No online server is connected."),12,MUTED,false),0);
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
        add(names,label(tr("ID dùng cho lượt mua tiếp theo","Recipient for next purchase"),12,MUTED,false),0);
        userRow.addView(names,new LinearLayout.LayoutParams(0,-2,1));
        TextView change=label(tr("Đổi ID ›","Edit ›"),14,PINK,true);
        change.setOnClickListener(v->editRecipient());
        userRow.addView(change);
        add(identity,userRow,0);
        add(content,identity,12);

        LinearLayout walletCard=card();
        add(walletCard,label(tr("VÍ COIN SHOP","COIN SHOP WALLET"),12,MUTED,true),6);
        add(walletCard,label(fmt(balance(user)),25,INK,true),6);
        add(walletCard,label(tr("Số dư nội bộ trên điện thoại này",
            "Local wallet balance on this device"),12,MUTED,false),4);
        add(walletCard,label(rateHint(),11,MUTED,false),0);
        add(content,walletCard,13);

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
        add(pay,label(tr("Phương thức thanh toán","Payment method"),17,INK,true),10);
        LinearLayout w=row();
        TextView mark=label("₫",19,PAPER,true);mark.setGravity(Gravity.CENTER);
        mark.setBackground(shape(Color.rgb(22,115,125),11,0));
        w.addView(mark,new LinearLayout.LayoutParams(dp(42),dp(42)));
        LinearLayout lines=col();
        add(lines,label(tr("Số dư ví app","App wallet balance"),16,INK,true),2);
        add(lines,label(fmt(balance(user)),13,MUTED,false),0);
        LinearLayout.LayoutParams linesP=new LinearLayout.LayoutParams(0,-2,1);
        linesP.leftMargin=dp(12);w.addView(lines,linesP);
        w.addView(label("✓",23,PINK,true));
        add(pay,w,0);
        add(content,pay,5);
        if(english)add(content,label(rateHint(),11,MUTED,false),9);
        add(content,action(tr("THANH TOÁN • ","PAY WITH WALLET • ")+
            fmt(price(selected)),true,()->confirm()),12);
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
        // Scroll entire custom sheet rather than using an empty weight-1 ScrollView.
        final android.app.Dialog dialog=new android.app.Dialog(this);
        ScrollView wrapper=new ScrollView(this);
        wrapper.setFillViewport(false);
        wrapper.setBackgroundColor(PAPER);
        LinearLayout sheet=col();
        sheet.setPadding(dp(16),dp(10),dp(16),dp(18));
        sheet.setBackground(shape(PAPER,20,0));
        wrapper.addView(sheet,new ScrollView.LayoutParams(-1,-2));

        LinearLayout header=row();
        TextView back=label("‹",30,INK,false);
        back.setGravity(Gravity.CENTER);
        header.addView(back,new LinearLayout.LayoutParams(dp(48),dp(44)));
        back.setOnClickListener(v->dialog.dismiss());
        TextView title=label(tr("Tùy chỉnh","Custom quantity"),20,INK,true);
        title.setGravity(Gravity.CENTER);
        header.addView(title,new LinearLayout.LayoutParams(0,dp(44),1));
        TextView question=label("?",23,INK,true);
        question.setGravity(Gravity.CENTER);
        header.addView(question,new LinearLayout.LayoutParams(dp(48),dp(44)));
        question.setOnClickListener(v->new AlertDialog.Builder(this)
            .setTitle(tr("Thông tin","Information"))
            .setMessage(tr("Chỉ thanh toán bằng ví app. Thanh toán ví không đồng nghĩa xu đã được nạp vào TikTok.",
                "Wallet payment is the only option. Coins are fulfilled separately."))
            .setPositiveButton("OK",(d,w)->{}).show());
        add(sheet,header,13);
        add(sheet,label(tr("Số lượng xu","Coin quantity"),17,INK,true),10);

        LinearLayout entry=row();entry.setPadding(dp(14),dp(9),dp(14),dp(9));
        entry.setBackground(shape(PALE,12,Color.rgb(217,222,231)));
        ImageView token=coin(38);
        LinearLayout.LayoutParams tokenParams=new LinearLayout.LayoutParams(dp(38),dp(38));
        tokenParams.rightMargin=dp(10);
        entry.addView(token,tokenParams);
        TextView number=label("",29,INK,true);
        number.setGravity(Gravity.CENTER_VERTICAL);
        entry.addView(number,new LinearLayout.LayoutParams(0,dp(52),1));
        add(sheet,entry,8);
        TextView cost=label("",16,MUTED,false);
        add(sheet,cost,12);

        StringBuilder digits=new StringBuilder(String.valueOf(selected));
        boolean[] replaceOnNext={true};
        TextView proceed=action("",true,()->{});
        Runnable update=()->{
            long n=0;
            try{n=Long.parseLong(digits.toString());}catch(Exception ignore){}
            number.setText(String.format(Locale.US,"%,d",n));
            boolean valid=n>0&&n<=2500000;
            cost.setText(tr("Tổng thanh toán: ","Total: ")+(valid?fmt(price((int)n)):"—"));
            proceed.setText(tr("THANH TOÁN VÍ • ","PAY WITH WALLET • ")+(valid?fmt(price((int)n)):"—"));
            proceed.setEnabled(valid);proceed.setAlpha(valid?1f:0.4f);
        };
        final String[][] rows={{"1","2","3","⌫"},{"4","5","6","000"},{"7","8","9","0"}};
        for(String[] keys:rows){
            LinearLayout keyRow=row();
            for(String value:keys){
                TextView b=label(value,23,INK,true);
                b.setGravity(Gravity.CENTER);
                b.setBackground(shape(PALE,9,0));
                LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(54),1);
                lp.setMargins(dp(3),dp(3),dp(3),dp(3));
                keyRow.addView(b,lp);
                b.setOnClickListener(v->{
                    if(value.equals("⌫")){
                        replaceOnNext[0]=false;
                        if(digits.length()>0)digits.deleteCharAt(digits.length()-1);
                    }else{
                        String candidate=replaceOnNext[0]?value:digits.toString()+value;
                        candidate=candidate.replaceFirst("^0+(?!$)","");
                        replaceOnNext[0]=false;
                        try{
                            long count=Long.parseLong(candidate);
                            if(count>2500000){
                                toast(tr("Tối đa 2.500.000 xu","Maximum 2,500,000 coins"));return;
                            }
                            digits.setLength(0);digits.append(candidate);
                        }catch(Exception ex){toast(tr("Số không hợp lệ","Invalid number"));return;}
                    }
                    update.run();
                });
                if(value.equals("⌫"))b.setOnLongClickListener(v->{
                    digits.setLength(0);replaceOnNext[0]=false;update.run();return true;
                });
            }
            add(sheet,keyRow,6);
        }
        add(sheet,label(tr("Thanh toán bằng số dư ví app","Payment via Coin Shop wallet"),15,INK,true),5);
        add(sheet,label(tr("Số dư hiện tại: ","Available: ")+fmt(balance(user)),13,MUTED,false),4);
        if(english)add(sheet,label(rateHint(),11,MUTED,false),8);
        proceed.setOnClickListener(v->{
            try{
                int n=Integer.parseInt(digits.toString());
                if(n<1||n>2500000){toast(tr("Số xu không hợp lệ","Invalid quantity"));return;}
                selected=n;dialog.dismiss();render();confirm();
            }catch(Exception ex){toast(tr("Vui lòng chọn số xu","Choose a coin amount"));}
        });
        add(sheet,proceed,2);
        update.run();
        dialog.setContentView(wrapper);
        dialog.show();
        android.view.Window window=dialog.getWindow();
        if(window!=null){
            window.setBackgroundDrawableResource(android.R.color.transparent);
            window.addFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            android.view.WindowManager.LayoutParams attrs=window.getAttributes();
            attrs.gravity=Gravity.BOTTOM;
            attrs.dimAmount=0.55f;
            window.setAttributes(attrs);
            int h=Math.min(dp(630),(int)(getResources().getDisplayMetrics().heightPixels*.87f));
            window.setLayout(android.view.WindowManager.LayoutParams.MATCH_PARENT,h);
        }
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
        long amount=price(selected);
        if(balance(user)<amount){
            new AlertDialog.Builder(this)
                .setTitle(tr("Số dư ví không đủ","Insufficient balance"))
                .setMessage(tr("Số dư: ","Balance: ")+fmt(balance(user))+"\n"+
                    tr("Cần thanh toán: ","Total: ")+fmt(amount)+"\n"+
                    tr("Liên hệ ADMIN để được cộng tiền vào ví.",
                       "Ask your ADMIN to credit your wallet."))
                .setPositiveButton("OK",(d,w)->{}).show();
            return;
        }
        new AlertDialog.Builder(this)
            .setTitle(tr("Xác nhận thanh toán ví","Confirm wallet payment"))
            .setMessage(tr("ID nhận xu: ","Recipient: ")+"@"+recipient+
                "\n"+tr("Số xu: ","Coins: ")+String.format(Locale.US,"%,d",selected)+
                "\n"+tr("Trừ từ ví: ","Wallet debit: ")+fmt(amount)+
                "\n"+tr("Số dư còn lại: ","Balance after: ")+fmt(balance(user)-amount)+
                "\n\n"+tr("Đơn sau khi thanh toán ví sẽ chờ nạp xu thủ công.",
                  "After wallet payment, coin delivery remains pending."))
            .setNegativeButton(tr("Hủy","Cancel"),(d,w)->{})
            .setPositiveButton(tr("Xác nhận thanh toán","Confirm payment"),(d,w)->payWallet()).show();
    }
    private long balance(String username){
        if(username==null||username.trim().isEmpty())return 0L;
        return Math.max(0L,db.getLong("wallet_vnd_"+username.toLowerCase(Locale.US),0L));
    }
    private String walletKey(String username){
        return "wallet_vnd_"+username.toLowerCase(Locale.US);
    }
    private JSONArray walletHistory(){
        try{return new JSONArray(db.getString("wallet_ledger","[]"));}catch(Exception ex){return new JSONArray();}
    }
    private void payWallet(){
        if(find(user)==null)return;
        long amount=price(selected),before=balance(user);
        if(amount<=0||before<amount){toast(tr("Số dư ví không đủ","Insufficient wallet balance"));render();return;}
        try{
            String id="CS-"+Long.toHexString(System.currentTimeMillis()).toUpperCase(Locale.US);
            JSONObject o=new JSONObject();
            o.put("id",id);o.put("buyer",user);o.put("recipient",recipient);
            o.put("coins",selected);o.put("vnd",amount);
            o.put("status","PAID_WALLET_PENDING_COINS");o.put("time",System.currentTimeMillis());
            JSONArray list=orders();list.put(o);
            JSONArray ledger=walletHistory();JSONObject log=new JSONObject();
            log.put("type","SPEND");log.put("user",user);log.put("by",user);
            log.put("amount",amount);log.put("order",id);log.put("time",System.currentTimeMillis());
            ledger.put(log);
            boolean saved=db.edit().putLong(walletKey(user),before-amount)
                .putString("orders",list.toString())
                .putString("wallet_ledger",ledger.toString()).commit();
            if(!saved){toast(tr("Không lưu được giao dịch","Failed to save transaction"));return;}
            lastCoins=selected;lastCost=amount;lastRecipient=recipient;lastOrder=id;
            tab="success";render();
        }catch(Exception ex){toast(tr("Không thanh toán được","Wallet payment failed"));}
    }
    private void successScreen(){
        LinearLayout c=card();c.setGravity(Gravity.CENTER_HORIZONTAL);
        c.setPadding(dp(16),dp(40),dp(16),dp(34));
        TextView check=label("✓",52,PAPER,true);
        check.setGravity(Gravity.CENTER);
        check.setBackground(shape(Color.rgb(46,193,154),80,0));
        LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(dp(100),dp(100));
        cp.gravity=Gravity.CENTER_HORIZONTAL;c.addView(check,cp);
        TextView head=label(tr("Thanh toán ví thành công!","Wallet payment successful!"),22,INK,true);
        head.setGravity(Gravity.CENTER);add(c,head,14);
        TextView detail=label(tr("Đã thanh toán ","Paid for ")+
            String.format(Locale.US,"%,d",lastCoins)+tr(" xu"," coins")+
            "\n"+fmt(lastCost)+"\n@"+lastRecipient,17,INK,false);
        detail.setGravity(Gravity.CENTER);add(c,detail,16);
        TextView status=label(tr("Đang chờ ADMIN nạp xu vào TikTok.",
            "Awaiting manual TikTok coin fulfillment."),14,MUTED,false);
        status.setGravity(Gravity.CENTER);add(c,status,14);
        add(c,action(tr("Hoàn thành","Done"),true,()->{tab="shop";render();}),10);
        add(c,action(tr("Xem giao dịch","View transaction"),false,()->{
            tab="history";render();
        }),0);
        add(content,c,12);
    }
    private void history(){
        add(content,label(tr("Lịch sử giao dịch","Transaction history"),23,INK,true),16);
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
            String raw=o.optString("status");
            String status=raw.equals("PAID_WALLET_PENDING_COINS")?
                tr("Đã trả bằng ví • chờ nạp xu","Wallet paid • coins pending"):
                raw.equals("COINS_DELIVERED")?
                tr("Đã xác nhận nạp xu","Coins marked delivered"):
                raw.equals("SIMULATED")?
                tr("Giao dịch cũ (thử nghiệm)","Old test transaction"):
                tr("Yêu cầu cũ • chưa thanh toán","Old unpaid request");
            add(c,label(status,13,PINK,true),7);
            add(c,label(new SimpleDateFormat("dd/MM/yyyy HH:mm",Locale.getDefault())
                .format(new Date(o.optLong("time"))),12,MUTED,false),0);
            if(all)add(c,label(tr("Tài khoản: ","User: ")+o.optString("buyer"),12,MUTED,false),0);
            add(content,c,10);
        }
        if(count==0)add(content,label(tr("Chưa có giao dịch","No transactions"),15,MUTED,false),10);
    }
    private void account(){
        add(content,label(tr("Tài khoản","Account"),23,INK,true),16);
        LinearLayout c=card();
        add(c,label(user,19,INK,true),6);
        add(c,label(admin?"ADMIN":tr("Khách hàng","Customer"),13,MUTED,false),13);
        add(c,label(tr("Số dư ví: ","Wallet balance: ")+fmt(balance(user)),19,INK,true),5);
        if(english)add(c,label(rateHint(),11,MUTED,false),12);
        add(c,action(tr("Đổi ID TikTok","Change TikTok ID"),false,()->editRecipient()),10);
        add(c,action(tr("Đăng xuất","Sign out"),true,()->{
            db.edit().remove("session").apply();loadSession();tab="shop";render();
        }),12);
        add(c,label(tr("Ví, tài khoản và lịch sử chỉ lưu trên thiết bị này. Đây không phải ví tiền thật có máy chủ.",
           "Wallet, accounts and history exist only on this device, not a server-backed cash wallet."),12,MUTED,false),0);
        add(content,c,10);
    }
    private void adminScreen(){
        add(content,label("ADMIN",23,INK,true),12);
        LinearLayout stats=card();
        add(stats,label(tr("Số dư mẫu ADMIN","ADMIN illustrative balance"),13,MUTED,true),5);
        add(stats,label("$10,000,000.00",24,INK,true),6);
        add(stats,label(tr("Số này chỉ để hiển thị, không phải tiền thật.",
            "This is display-only, not real money."),12,MUTED,false),10);
        add(stats,label(tr("Ví ADM trên máy: ","ADM local wallet: ")+fmt(balance(user)),17,INK,true),5);
        if(english)add(stats,label(rateHint(),11,MUTED,false),10);
        add(stats,label(tr("Số tài khoản trên máy: ","Local users: ")+users().length(),13,MUTED,false),0);
        add(content,stats,13);

        LinearLayout credit=card();
        add(credit,label(tr("Cộng tiền ví người dùng","Credit a user's wallet"),19,INK,true),6);
        if(english)add(credit,label(rateHint(),11,MUTED,false),8);
        add(credit,label(tr("Chỉ áp dụng cho tài khoản tạo trên điện thoại này.",
             "Works only for accounts on this device."),12,MUTED,false),13);
        EditText who=field(tr("Tên đăng nhập nhận tiền","Recipient username"),false);
        EditText amount=field(tr("Số tiền cộng (VND)","Credit amount (USD, e.g. 10.50)"),false);
        amount.setInputType(InputType.TYPE_CLASS_NUMBER);
        add(credit,who,9);add(credit,amount,12);
        add(credit,action(tr("CỘNG TIỀN VÀO VÍ","CREDIT WALLET"),true,()->{
            String target=who.getText().toString().trim();
            JSONObject found=find(target);
            if(!admin||found==null){
                toast(tr("Không tìm thấy tài khoản trên máy","Local account not found"));return;
            }
            long increment;
            try{
                String value=amount.getText().toString().trim();
                if(english)increment=new BigDecimal(value)
                    .multiply(BigDecimal.valueOf(VND_PER_USD))
                    .setScale(0,RoundingMode.HALF_UP).longValueExact();
                else increment=Long.parseLong(value);
            }catch(Exception ex){
                toast(tr("Số tiền không hợp lệ","Invalid amount"));return;
            }
            if(increment<1||increment>1000000000L||balance(found.optString("name"))>900000000000L-increment){
                toast(tr("Số tiền từ 1 đến 1 tỷ đồng","Amount must be 1 to 1 billion VND"));return;
            }
            String targetUser=found.optString("name");
            new AlertDialog.Builder(this)
                .setTitle(tr("Xác nhận cộng ví","Confirm wallet credit"))
                .setMessage(tr("Tài khoản: ","Account: ")+targetUser+
                    "\n"+tr("Cộng: ","Credit: ")+fmt(increment)+
                    "\n"+tr("Số dư mới: ","New balance: ")+fmt(balance(targetUser)+increment))
                .setNegativeButton(tr("Hủy","Cancel"),(d,w)->{})
                .setPositiveButton(tr("Xác nhận","Confirm"),(d,w)->{
                    creditWallet(targetUser,increment);
                }).show();
        }),10);
        add(content,credit,13);

        LinearLayout adm=card();
        add(adm,label(tr("Tài khoản quản trị ADM","ADM administrator account"),17,INK,true),6);
        add(adm,label(tr("Mật khẩu được tạo riêng trên điện thoại, không lưu vào GitHub.",
            "Choose the admin password locally; it is not committed to GitHub."),12,MUTED,false),12);
        add(adm,action(tr("Tạo hoặc đổi mật khẩu ADM","Set up / reset ADM credentials"),
            false,()->setupAdm()),0);
        add(content,adm,16);

        add(content,label(tr("Danh sách người dùng trên máy","Local users"),18,INK,true),12);
        JSONArray ua=users();
        for(int i=0;i<ua.length();i++){
            JSONObject u=ua.optJSONObject(i);if(u==null)continue;
            String n=u.optString("name");
            LinearLayout line=card();
            add(line,label(n+(u.optBoolean("admin")?" • ADMIN":""),15,INK,true),3);
            add(line,label(fmt(balance(n)),13,MUTED,false),0);
            add(content,line,7);
        }
        add(content,label(tr("Lịch sử đơn trên máy","Local orders"),18,INK,true),12);
        showOrders(true);
    }
    private void creditWallet(String target,long increment){
        if(!admin||find(target)==null)return;
        long current=balance(target);
        try{
            JSONArray ledger=walletHistory();
            JSONObject entry=new JSONObject();
            entry.put("type","CREDIT");entry.put("user",target);
            entry.put("by",user);entry.put("amount",increment);
            entry.put("time",System.currentTimeMillis());
            ledger.put(entry);
            boolean ok=db.edit().putLong(walletKey(target),current+increment)
                .putString("wallet_ledger",ledger.toString()).commit();
            if(ok){toast(tr("Đã cộng tiền vào ví","Wallet credited"));render();}
            else toast(tr("Không lưu được","Save failed"));
        }catch(Exception ex){toast(tr("Lỗi khi cộng ví","Wallet credit failed"));}
    }
    private void setupAdm(){
        if(!admin)return;
        EditText pwd=field(tr("Mật khẩu mới cho ADM (ít nhất 8 ký tự)",
            "New ADM password (8+ chars)"),true);
        LinearLayout box=col();space(box,17,10);add(box,pwd,0);
        new AlertDialog.Builder(this)
            .setTitle(tr("Thiết lập tài khoản ADM","Set up ADM login"))
            .setView(box)
            .setNegativeButton(tr("Hủy","Cancel"),(d,w)->{})
            .setPositiveButton(tr("Lưu ADM","Save ADM"),(d,w)->{
                String pass=pwd.getText().toString();
                if(pass.length()<8){toast(tr("Mật khẩu cần ít nhất 8 ký tự",
                    "Password needs 8+ characters"));return;}
                try{
                    JSONArray arr=users();String key=salt();
                    JSONObject found=null;
                    for(int i=0;i<arr.length();i++){
                        JSONObject u=arr.optJSONObject(i);
                        if(u!=null&&"ADM".equalsIgnoreCase(u.optString("name")))found=u;
                    }
                    if(found==null){found=new JSONObject();found.put("name","ADM");arr.put(found);}
                    found.put("admin",true);found.put("salt",key);found.put("hash",hash(pass,key));
                    db.edit().putString("users",arr.toString()).apply();
                    toast(tr("Đã thiết lập ADM trên máy","ADM updated locally"));render();
                }catch(Exception ex){toast(tr("Không lưu được ADM","Cannot save ADM"));}
            }).show();
    }
}
