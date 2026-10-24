package vn.dieutuan.trystyle;
import android.app.*;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.net.*;
import android.os.*;
import android.text.InputType;
import android.util.Base64;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;

public class MainActivity extends Activity {
 static final int BG=0xff101225,CARD=0xff242742,WHITE=0xfff9f7ff,MUTED=0xffb9c0d9,PINK=0xffff74a7;
 final ExecutorService pool=Executors.newSingleThreadExecutor();
 SharedPreferences prefs; String host="",token="",screen="auth",activeJob="";
 JSONObject user=null; JSONArray members=new JSONArray(),jobs=new JSONArray();
 Bitmap person,cloth,result; boolean agreed=false,aiReady=false;
 LinearLayout page;
 interface Callback{void done(JSONObject reply,String error);}
 int dp(int x){return (int)(x*getResources().getDisplayMetrics().density+.5f);}
 @Override public void onCreate(Bundle s){super.onCreate(s);
  getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);
  prefs=getSharedPreferences("trystyle_data",MODE_PRIVATE);
  host=prefs.getString("host","");token=prefs.getString("token","");
  if(!token.isEmpty() && !host.isEmpty()){screen="home";refresh();}draw();
 }
 @Override public void onDestroy(){pool.shutdown();super.onDestroy();}
 GradientDrawable shape(int c,int r){GradientDrawable d=new GradientDrawable();d.setColor(c);d.setCornerRadius(dp(r));return d;}
 TextView text(String s,int size,int color,boolean bold){
  TextView v=new TextView(this);v.setText(s);v.setTextSize(size);v.setTextColor(color);
  if(bold)v.setTypeface(null,1);return v;
 }
 LinearLayout col(){LinearLayout l=new LinearLayout(this);l.setOrientation(1);return l;}
 void add(LinearLayout p,View v,int margin){
  LinearLayout.LayoutParams a=new LinearLayout.LayoutParams(-1,-2);a.bottomMargin=dp(margin);p.addView(v,a);
 }
 LinearLayout card(){LinearLayout c=col();c.setPadding(dp(17),dp(17),dp(17),dp(17));c.setBackground(shape(CARD,18));return c;}
 EditText field(String hint,boolean secret){
  EditText e=new EditText(this);e.setTextColor(WHITE);e.setHintTextColor(MUTED);e.setTextSize(15);
  e.setSingleLine(true);e.setHint(hint);e.setBackground(shape(0xff333650,12));
  e.setPadding(dp(12),dp(12),dp(12),dp(12));if(secret)e.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);
  return e;
 }
 TextView button(String title,boolean primary,Runnable run){
  TextView t=text(title,15,primary?BG:WHITE,true);t.setGravity(Gravity.CENTER);
  t.setPadding(dp(12),dp(14),dp(12),dp(14));t.setBackground(shape(primary?PINK:0xff343c59,12));
  t.setOnClickListener(v->run.run());return t;
 }
 void toast(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
 boolean isAdmin(){return user!=null&&"admin".equals(user.optString("role"));}
 void draw(){
  ScrollView sv=new ScrollView(this);sv.setBackgroundColor(BG);setContentView(sv);
  page=col();page.setPadding(dp(18),dp(24),dp(18),dp(28));sv.addView(page);
  add(page,text("TRY STYLE AI",26,WHITE,true),4);
  add(page,text("Thử đồ bằng ảnh • Thành viên online",13,MUTED,false),16);
  if(host.isEmpty() && !"config".equals(screen)){
   LinearLayout c=card();add(c,text("Chưa kết nối máy chủ online",19,PINK,true),9);
   add(c,text("Phải triển khai Worker + D1 và nhập URL thật. Chưa thể đăng ký chung nhiều máy khi chưa triển khai.",13,MUTED,false),12);
   add(c,button("CẤU HÌNH MÁY CHỦ",true,()->{screen="config";draw();}),0);add(page,c,13);
  }
  if("config".equals(screen)){config();return;}
  if(user==null){auth();return;}
  add(page,button("THỬ ĐỒ",false,()->{screen="home";draw();}),8);
  if(isAdmin())add(page,button("QUẢN TRỊ VIÊN ADM",true,()->loadUsers()),9);
  add(page,button("LỊCH SỬ",false,()->loadHistory()),12);
  if("admin".equals(screen))adminPage();
  else if("history".equals(screen))historyPage();
  else if("job".equals(screen))jobPage();
  else homePage();
  add(page,button("ĐĂNG XUẤT",false,()->{token="";user=null;prefs.edit().remove("token").apply();screen="auth";draw();}),8);
  if(isAdmin())add(page,button("CÀI ĐẶT MÁY CHỦ (ADM)",false,()->{screen="config";draw();}),0);
 }
 void config(){
  LinearLayout c=card();add(c,text("Địa chỉ API máy chủ",20,WHITE,true),8);
  add(c,text("Ví dụ: https://trystyle-ai-api.example.workers.dev. Chỉ chấp nhận HTTPS.",13,MUTED,false),12);
  EditText e=field("https://...workers.dev",false);e.setText(host);add(c,e,12);
  add(c,button("KIỂM TRA VÀ LƯU",true,()->{
   String s=e.getText().toString().trim().replaceAll("/+$","");
   if(!s.matches("https://[A-Za-z0-9._:-]+")){toast("URL HTTPS không hợp lệ");return;}
   // Only persist the URL after a genuine TryStyle AI health response.
   final String previousHost=host;
   host=s;
   api("GET","/api/health",null,(j,err)->{
    if(err!=null || !j.optBoolean("ok",false) || !"TryStyle AI".equals(j.optString("service"))){
     host=previousHost;
     toast(err!=null?err:"Đây không phải máy chủ TryStyle AI");
     screen="config";
    }else{
     token="";user=null;
     prefs.edit().putString("host",s).remove("token").apply();
     toast("Đã kết nối máy chủ. Cấu hình đã được ẩn.");
     screen="auth";
    }
    draw();
   });
  }),10);
  add(c,button("QUAY LẠI",false,()->{screen=user==null?"auth":"home";draw();}),0);add(page,c,12);
 }
 void auth(){
  LinearLayout c=card();add(c,text("Đăng nhập / Tự đăng ký",22,WHITE,true),8);
  add(c,text("Tài khoản thành viên tự tạo. ADM được tạo riêng trên máy chủ, không cho đăng ký qua app.",13,MUTED,false),12);
  EditText name=field("Tên tài khoản (3–24 ký tự)",false),pass=field("Mật khẩu (10+ ký tự)",true);
  add(c,name,10);add(c,pass,13);
  add(c,button("ĐĂNG NHẬP",true,()->login(false,name,pass)),9);
  add(c,button("ĐĂNG KÝ THÀNH VIÊN",false,()->login(true,name,pass)),9);
  if(host.isEmpty())add(c,button("CẤU HÌNH MÁY CHỦ",false,()->{screen="config";draw();}),0);add(page,c,12);
 }
 void login(boolean create,EditText name,EditText pass){
  if(host.isEmpty()){screen="config";draw();return;}
  JSONObject data=new JSONObject();
  try{data.put("username",name.getText().toString().trim());data.put("password",pass.getText().toString());}catch(Exception ignored){}
  api("POST",create?"/api/register":"/api/login",data,(j,e)->{
   if(e!=null){toast(e);return;}
   token=j.optString("token");user=j.optJSONObject("user");
   prefs.edit().putString("token",token).apply();screen="home";refresh();
  });
 }
 void refresh(){
  if(token.isEmpty())return;
  api("GET","/api/me",null,(j,e)->{
   if(e!=null){toast(e);if(e.contains("sign in")||e.contains("Session")){user=null;token="";prefs.edit().remove("token").apply();screen="auth";draw();}return;}
   user=j.optJSONObject("user");aiReady=j.optBoolean("ai_enabled",false);draw();
  });
 }
 void homePage(){
  LinearLayout c=card();
  add(c,text("Xin chào, "+user.optString("username"),20,WHITE,true),8);
  add(c,text("Lượt AI còn lại: "+user.optInt("credits"),24,PINK,true),8);
  boolean vip=user.optBoolean("vip_lifetime")||user.optLong("vip_until")>System.currentTimeMillis();
  add(c,text(vip?"VIP đang hoạt động":"Thành viên thường",13,MUTED,false),0);add(page,c,12);
  LinearLayout tryon=card();add(tryon,text("THỬ QUẦN ÁO",21,WHITE,true),9);
  add(tryon,text("Chọn ảnh người và ảnh trang phục, sau đó bấm tạo ảnh AI.",13,MUTED,false),10);
  add(tryon,button("1. CHỌN ẢNH NGƯỜI",false,()->pick(1)),8);
  if(person!=null)add(tryon,picture(person),12);
  add(tryon,button("2. CHỌN ẢNH QUẦN ÁO",false,()->pick(2)),8);
  if(cloth!=null)add(tryon,picture(cloth),12);
  add(tryon,button((agreed?"☑":"☐")+" Đồng ý gửi hai ảnh tới FASHN AI",false,()->{agreed=!agreed;draw();}),11);
  add(tryon,button("TẠO ẢNH THỬ ĐỒ AI",true,()->submit()),8);
  add(tryon,text(aiReady?"AI đã kết nối, mỗi ảnh tốn 1 lượt.":"AI CHƯA KẾT NỐI: máy chủ cần khóa FASHN_API_KEY.",12,aiReady?0xff7ae4bc:MUTED,false),0);
  add(page,tryon,12);
 }
 ImageView picture(Bitmap b){ImageView i=new ImageView(this);i.setImageBitmap(b);
  i.setScaleType(ImageView.ScaleType.FIT_CENTER);i.setLayoutParams(new LinearLayout.LayoutParams(-1,dp(180)));return i;}
 void pick(int code){
  Intent intent=new Intent(Intent.ACTION_GET_CONTENT);intent.setType("image/*");
  try{startActivityForResult(Intent.createChooser(intent,"Chọn ảnh"),code);}
  catch(Exception e){toast("Không mở được bộ chọn ảnh");}
 }
 @Override protected void onActivityResult(int id,int rc,Intent data){
  super.onActivityResult(id,rc,data);if(rc!=RESULT_OK||data==null||data.getData()==null)return;
  Uri uri=data.getData();pool.submit(()->{
   Bitmap b=null;
   try{
    BitmapFactory.Options o=new BitmapFactory.Options();o.inJustDecodeBounds=true;
    try(InputStream i=getContentResolver().openInputStream(uri)){BitmapFactory.decodeStream(i,null,o);}
    int sample=1;while(o.outWidth/sample>1100||o.outHeight/sample>1100)sample*=2;
    o.inSampleSize=sample;o.inJustDecodeBounds=false;
    try(InputStream i=getContentResolver().openInputStream(uri)){b=BitmapFactory.decodeStream(i,null,o);}
   }catch(Exception ignored){}
   final Bitmap image=b;runOnUiThread(()->{
    if(image==null){toast("Ảnh không đọc được");return;}
    if(id==1)person=image;else if(id==2)cloth=image;draw();
   });
  });
 }
 String encoded(Bitmap original){
  Bitmap b=original;int w=b.getWidth(),h=b.getHeight();
  if(w>1100||h>1100){double s=Math.min(1100d/w,1100d/h);
   b=Bitmap.createScaledBitmap(b,(int)(w*s),(int)(h*s),true);}
  ByteArrayOutputStream bytes=new ByteArrayOutputStream();
  for(int q=80;q>=40;q-=10){bytes.reset();b.compress(Bitmap.CompressFormat.JPEG,q,bytes);if(bytes.size()<850000)break;}
  return "data:image/jpeg;base64,"+Base64.encodeToString(bytes.toByteArray(),Base64.NO_WRAP);
 }
 void submit(){
  if(person==null||cloth==null){toast("Phải chọn đủ hai ảnh");return;}
  if(!agreed){toast("Hãy xác nhận quyền sử dụng và đồng ý gửi ảnh");return;}
  if(!aiReady){toast("Máy chủ chưa kết nối FASHN AI");return;}
  if(user.optInt("credits")<1){toast("Hết lượt AI, liên hệ ADM cấp thêm");return;}
  toast("Đang gửi ảnh...");
  Bitmap p=person,c=cloth;
  pool.submit(()->{
   JSONObject payload=new JSONObject();
   try{payload.put("person_image",encoded(p));payload.put("garment_image",encoded(c));}catch(Exception ignored){}
   request("POST","/api/tryon",payload,(j,error)->{
    if(error!=null){toast(error);return;}
    JSONObject job=j.optJSONObject("job");
    activeJob=job==null?"":job.optString("id");result=null;
    screen="job";draw();refresh();
   });
  });
 }
 void loadHistory(){api("GET","/api/tryons",null,(j,e)->{
  if(e!=null){toast(e);return;}jobs=j.optJSONArray("jobs");if(jobs==null)jobs=new JSONArray();
  screen="history";draw();
 });}
 void historyPage(){
  add(page,text("LỊCH SỬ THỬ ĐỒ",21,WHITE,true),12);
  for(int x=0;x<jobs.length();x++){
   JSONObject j=jobs.optJSONObject(x);if(j==null)continue;
   String id=j.optString("id");LinearLayout c=card();
   add(c,text(j.optString("status")+"  •  "+id,13,MUTED,false),8);
   add(c,button("XEM KẾT QUẢ",false,()->{activeJob=id;screen="job";result=null;draw();checkJob();}),0);
   add(page,c,8);
  }
 }
 void jobPage(){
  LinearLayout c=card();add(c,text("KẾT QUẢ THỬ ĐỒ",21,WHITE,true),8);
  add(c,text("Đơn: "+activeJob,13,MUTED,false),11);
  if(result!=null)add(c,picture(result),12);
  add(c,button("KIỂM TRA TRẠNG THÁI",true,()->checkJob()),10);
  add(c,button("THỬ TIẾP",false,()->{screen="home";draw();}),0);add(page,c,12);
 }
 void checkJob(){
  if(activeJob.isEmpty())return;
  api("GET","/api/tryon/"+activeJob,null,(r,e)->{
   if(e!=null){toast(e);return;}JSONObject job=r.optJSONObject("job");
   if(job==null)return;
   String status=job.optString("status");
   if("failed".equals(status)){toast("AI thất bại, hệ thống hoàn trả lượt");refresh();}
   else if("completed".equals(status)){
    String imageUrl=job.optString("result_url","");
    if(!imageUrl.startsWith("https://")){toast("Không có ảnh kết quả");return;}
    pool.submit(()->{
     Bitmap bmp=null;
     try(InputStream s=new URL(imageUrl).openStream()){bmp=BitmapFactory.decodeStream(s);}catch(Exception ignored){}
     final Bitmap image=bmp;runOnUiThread(()->{result=image;screen="job";draw();});
    });
   }else toast("Đang xử lý, kiểm tra lại sau.");
  });
 }
 void loadUsers(){api("GET","/api/admin/users",null,(j,e)->{
  if(e!=null){toast(e);return;}
  members=j.optJSONArray("users");if(members==null)members=new JSONArray();
  screen="admin";draw();
 });}
 void adminPage(){
  add(page,text("QUẢN TRỊ VIÊN • ADM",23,PINK,true),10);
  add(page,text("Danh sách thành viên từ máy chủ chung",13,MUTED,false),11);
  add(page,button("LÀM MỚI DANH SÁCH",true,()->loadUsers()),12);
  for(int i=0;i<members.length();i++){
   JSONObject u=members.optJSONObject(i);if(u==null)continue;
   LinearLayout c=card();
   add(c,text(u.optString("username")+" • "+u.optString("role"),18,WHITE,true),6);
   add(c,text("Lượt "+u.optInt("credits")+" | "+(u.optBoolean("locked")?"Đã khóa":"Hoạt động")+
    " | VIP "+(u.optBoolean("vip_lifetime")?"Vĩnh viễn":u.optLong("vip_until")>System.currentTimeMillis()?"Còn hạn":"Chưa có"),13,MUTED,false),11);
   if("member".equals(u.optString("role")))add(c,button("QUẢN LÝ",false,()->manage(u)),0);
   add(page,c,9);
  }
 }
 void manage(JSONObject u){
  String[] choices={"Cộng 10 lượt","Cộng 50 lượt","Cộng 100 lượt","VIP 7 ngày","VIP 30 ngày","VIP 90 ngày","VIP vĩnh viễn","Xóa VIP",u.optBoolean("locked")?"Mở khóa":"Khóa"};
  new AlertDialog.Builder(this).setTitle("Quản lý "+u.optString("username"))
   .setItems(choices,(d,choice)->{
    String endpoint=choice<3?"/api/admin/credit":choice<8?"/api/admin/vip":"/api/admin/lock";
    JSONObject request=new JSONObject();
    try{
     request.put("user_id",u.optString("id"));
     if(choice<3)request.put("amount",new int[]{10,50,100}[choice]);
     else if(choice<8)request.put("days",new int[]{7,30,90,-1,0}[choice-3]);
     else request.put("lock",!u.optBoolean("locked"));
    }catch(Exception ignored){}
    new AlertDialog.Builder(this).setTitle("Xác nhận "+choices[choice])
     .setMessage("Áp dụng cho "+u.optString("username")+"?")
     .setNegativeButton("Hủy",(v,z)->{})
     .setPositiveButton("Xác nhận",(v,z)->api("POST",endpoint,request,(a,e)->{
      if(e!=null)toast(e);else{toast("Đã cập nhật");loadUsers();}
     })).show();
   }).show();
 }
 void api(String method,String path,JSONObject payload,Callback cb){
  pool.submit(()->request(method,path,payload,cb));
 }
 void request(String method,String path,JSONObject payload,Callback cb){
  String err=null;JSONObject result=new JSONObject();
  try{
   if(!host.startsWith("https://"))throw new Exception("Hãy cấu hình máy chủ HTTPS");
   HttpURLConnection con=(HttpURLConnection)new URL(host+path).openConnection();
   con.setRequestMethod(method);con.setConnectTimeout(16000);con.setReadTimeout(45000);
   con.setRequestProperty("Accept","application/json");
   if(!token.isEmpty())con.setRequestProperty("Authorization","Bearer "+token);
   if(payload!=null){
    con.setDoOutput(true);con.setRequestProperty("Content-Type","application/json");
    try(OutputStream out=con.getOutputStream()){out.write(payload.toString().getBytes(StandardCharsets.UTF_8));}
   }
   int status=con.getResponseCode();
   try(InputStream in=status>=400?con.getErrorStream():con.getInputStream()){
    if(in==null)throw new Exception("Máy chủ không phản hồi");
    ByteArrayOutputStream bytes=new ByteArrayOutputStream();
    byte[] buffer=new byte[8192];int n;
    while((n=in.read(buffer))!=-1){bytes.write(buffer,0,n);
     if(bytes.size()>2000000)throw new Exception("Phản hồi quá lớn");}
    result=new JSONObject(new String(bytes.toByteArray(),StandardCharsets.UTF_8));
   }
   if(status>=400)err=result.optString("error","HTTP "+status);
   con.disconnect();
  }catch(Exception e){err=e.getMessage();}
  final String error=err;final JSONObject j=result;
  runOnUiThread(()->cb.done(j,error));
 }
}