// TryStyle AI online backend. Deploy with Wrangler + D1.
// No client secrets, phone-local user databases, or mock "successful" AI generations.
class ApiError extends Error {
  constructor(code, message){super(message);this.code=code;}
}
const json=(data,status=200)=>new Response(JSON.stringify(data),{status,headers:{
  "content-type":"application/json; charset=utf-8","cache-control":"no-store",
  "x-content-type-options":"nosniff","access-control-allow-origin":"*"
}});
const hex=(bytes)=>[...bytes].map(x=>x.toString(16).padStart(2,"0")).join("");
const random=(size=24)=>hex(crypto.getRandomValues(new Uint8Array(size)));
const sha=async(value)=>hex(new Uint8Array(await crypto.subtle.digest("SHA-256",new TextEncoder().encode(value))));
async function passwordHash(password,salt){
  const k=await crypto.subtle.importKey("raw",new TextEncoder().encode(password),"PBKDF2",false,["deriveBits"]);
  return hex(new Uint8Array(await crypto.subtle.deriveBits({
    name:"PBKDF2",salt:new TextEncoder().encode(salt),iterations:210000,hash:"SHA-256"
  },k,256)));
}
async function input(request,limit=3400000){
  const length=Number(request.headers.get("content-length")||0);
  if(length>limit)throw new ApiError(413,"Request too large");
  const body=await request.text();
  if(body.length>limit)throw new ApiError(413,"Request too large");
  try{return JSON.parse(body);}catch{throw new ApiError(400,"Invalid JSON");}
}
function nameValid(s){return typeof s==="string"&&/^[A-Za-z0-9._]{3,24}$/.test(s);}
function passwordValid(s){return typeof s==="string"&&s.length>=10&&s.length<=128;}
const publicUser=(u)=>({
  id:u.id,username:u.username,role:u.role,credits:u.credits,
  vip_until:u.vip_until, vip_lifetime:!!u.vip_lifetime,
  locked:!!u.locked,created_at:u.created_at
});
async function ensureAdmin(env){
  const existing=await env.DB.prepare("SELECT id FROM users WHERE username=?").bind("ADM").first();
  if(existing)return;
  const pwd=env.ADMIN_BOOTSTRAP_PASSWORD;
  if(!passwordValid(pwd))throw new ApiError(503,"Server admin credential not configured");
  const salt=random(16),passhash=await passwordHash(pwd,salt);
  await env.DB.prepare("INSERT OR IGNORE INTO users(id,username,salt,passhash,role,credits,vip_until,vip_lifetime,locked,created_at) VALUES(?,?,?,?,?,0,0,0,0,?)")
    .bind(crypto.randomUUID(),"ADM",salt,passhash,"admin",Date.now()).run();
}
async function currentUser(request,env){
  const h=request.headers.get("authorization")||"";
  const match=/^Bearer ([a-f0-9]{48})$/.exec(h);
  if(!match)throw new ApiError(401,"Please sign in");
  const tokenHash=await sha(match[1]);
  const now=Date.now();
  const user=await env.DB.prepare("SELECT u.* FROM sessions s JOIN users u ON u.id=s.user_id WHERE s.token_hash=? AND s.expires_at>?")
    .bind(tokenHash,now).first();
  if(!user)throw new ApiError(401,"Session expired; sign in again");
  if(user.locked)throw new ApiError(403,"Account locked");
  return user;
}
function adminOnly(u){if(u.role!=="admin")throw new ApiError(403,"ADM access required");}
async function newSession(env,userId){
  const raw=random(24);
  await env.DB.prepare("INSERT INTO sessions(token_hash,user_id,expires_at) VALUES(?,?,?)")
    .bind(await sha(raw),userId,Date.now()+30*86400000).run();
  return raw;
}
async function requestProvider(env,body){
  const response=await fetch("https://api.fashn.ai/v1/run",{
    method:"POST",headers:{"authorization":"Bearer "+env.FASHN_API_KEY,"content-type":"application/json"},
    body:JSON.stringify({model_name:"tryon-v1.6",inputs:{
      model_image:body.person_image,garment_image:body.garment_image,
      category:"auto",moderation_level:"conservative",mode:"balanced",num_samples:1,
      output_format:"jpeg"
    }})
  });
  const j=await response.json().catch(()=>({}));
  if(!response.ok||!j.id)throw new ApiError(502,j.error?.message||"AI provider could not start generation");
  return j.id;
}
function checkImage(value){
  return typeof value==="string"&&
    /^data:image\/(jpeg|png|webp);base64,[A-Za-z0-9+/=]+$/.test(value)&&value.length<1600000;
}
async function reconcile(env,job){
  if(!env.FASHN_API_KEY||!job.provider_id||["completed","failed"].includes(job.status))return job;
  const r=await fetch("https://api.fashn.ai/v1/status/"+encodeURIComponent(job.provider_id),{
    headers:{"authorization":"Bearer "+env.FASHN_API_KEY}});
  if(!r.ok)return job;
  const status=await r.json();
  if(status.status==="completed"&&Array.isArray(status.output)&&typeof status.output[0]==="string"){
    const url=status.output[0];
    if(!url.startsWith("https://"))throw new ApiError(502,"Invalid image result");
    await env.DB.prepare("UPDATE tryons SET status='completed',result_url=?,updated_at=? WHERE id=? AND status NOT IN ('completed','failed')")
      .bind(url,Date.now(),job.id).run();
  }else if(["failed","canceled","error"].includes(status.status)){
    const update=await env.DB.prepare("UPDATE tryons SET status='failed',updated_at=? WHERE id=? AND status NOT IN ('completed','failed')")
      .bind(Date.now(),job.id).run();
    if(update.meta?.changes===1&&job.credits_spent===1){
      await env.DB.prepare("UPDATE users SET credits=credits+1 WHERE id=?").bind(job.user_id).run();
    }
  }
  return await env.DB.prepare("SELECT * FROM tryons WHERE id=?").bind(job.id).first();
}
const publicJob=(j)=>({id:j.id,status:j.status,result_url:j.result_url,created_at:j.created_at});
export default {
  async fetch(request,env){
    if(request.method==="OPTIONS")return new Response(null,{status:204,headers:{
      "access-control-allow-origin":"*","access-control-allow-headers":"authorization, content-type",
      "access-control-allow-methods":"GET, POST, OPTIONS"
    }});
    try{
      const url=new URL(request.url),path=url.pathname,method=request.method;
      if(method==="GET"&&path==="/api/health")
        return json({ok:true,service:"TryStyle AI",version:"1.0",ai_enabled:!!env.FASHN_API_KEY});
      if(!path.startsWith("/api/"))throw new ApiError(404,"Not found");
      if(!env.DB)throw new ApiError(503,"Cloudflare D1 binding not configured");
      await ensureAdmin(env);
      if(method==="POST"&&path==="/api/register"){
        const b=await input(request,10000),username=String(b.username||"").trim(),password=b.password;
        if(!nameValid(username)||username.toUpperCase()==="ADM")throw new ApiError(400,"Username must be 3–24 letters/numbers and not ADM");
        if(!passwordValid(password))throw new ApiError(400,"Password must be 10–128 characters");
        const salt=random(16),passhash=await passwordHash(password,salt),id=crypto.randomUUID();
        try{
          await env.DB.prepare("INSERT INTO users(id,username,salt,passhash,role,credits,vip_until,vip_lifetime,locked,created_at) VALUES(?,?,?,?,?,0,0,0,0,?)")
            .bind(id,username,salt,passhash,"member",Date.now()).run();
        }catch{throw new ApiError(409,"Username already registered");}
        const u=await env.DB.prepare("SELECT * FROM users WHERE id=?").bind(id).first();
        return json({token:await newSession(env,id),user:publicUser(u)},201);
      }
      if(method==="POST"&&path==="/api/login"){
        const b=await input(request,10000),username=String(b.username||"").trim(),password=b.password;
        if(!nameValid(username)||typeof password!=="string")throw new ApiError(400,"Invalid login");
        const u=await env.DB.prepare("SELECT * FROM users WHERE username=?").bind(username).first();
        if(!u)throw new ApiError(401,"Username or password incorrect");
        const digest=await passwordHash(password,u.salt);
        if(digest!==u.passhash)throw new ApiError(401,"Username or password incorrect");
        if(u.locked)throw new ApiError(403,"Account locked");
        return json({token:await newSession(env,u.id),user:publicUser(u)});
      }
      const me=await currentUser(request,env);
      if(method==="GET"&&path==="/api/me")return json({user:publicUser(me),ai_enabled:!!env.FASHN_API_KEY});
      if(method==="POST"&&path==="/api/logout"){
        const tok=request.headers.get("authorization").slice(7);
        await env.DB.prepare("DELETE FROM sessions WHERE token_hash=?").bind(await sha(tok)).run();
        return json({ok:true});
      }
      if(method==="POST"&&path==="/api/change-password"){
        const b=await input(request,10000);
        if(!passwordValid(b.new_password))throw new ApiError(400,"Password must be 10–128 characters");
        const old=await passwordHash(String(b.old_password||""),me.salt);
        if(old!==me.passhash)throw new ApiError(403,"Current password incorrect");
        const salt=random(16);
        await env.DB.prepare("UPDATE users SET salt=?,passhash=? WHERE id=?")
          .bind(salt,await passwordHash(b.new_password,salt),me.id).run();
        await env.DB.prepare("DELETE FROM sessions WHERE user_id=?").bind(me.id).run();
        return json({ok:true,message:"Password changed. Sign in again"});
      }
      if(method==="GET"&&path==="/api/admin/users"){
        adminOnly(me);
        const u=await env.DB.prepare("SELECT id,username,role,credits,vip_until,vip_lifetime,locked,created_at FROM users ORDER BY created_at DESC LIMIT 200").all();
        return json({users:u.results.map(publicUser)});
      }
      if(method==="POST"&&path==="/api/admin/credit"){
        adminOnly(me);
        const b=await input(request,10000),count=Number(b.amount);
        if(!Number.isSafeInteger(count)||count<1||count>10000)throw new ApiError(400,"Credit must be 1–10000");
        const upd=await env.DB.prepare("UPDATE users SET credits=credits+? WHERE id=? AND role='member'")
          .bind(count,b.user_id).run();
        if(upd.meta?.changes!==1)throw new ApiError(404,"Member not found");
        return json({ok:true});
      }
      if(method==="POST"&&path==="/api/admin/vip"){
        adminOnly(me);
        const b=await input(request,10000),days=Number(b.days);
        if(![0,7,30,90,-1].includes(days))throw new ApiError(400,"VIP days must be 0, 7, 30, 90 or -1");
        const now=Date.now(),until=days>0?now+days*86400000:0;
        const upd=await env.DB.prepare("UPDATE users SET vip_until=?,vip_lifetime=? WHERE id=? AND role='member'")
          .bind(until,days===-1?1:0,b.user_id).run();
        if(upd.meta?.changes!==1)throw new ApiError(404,"Member not found");
        return json({ok:true});
      }
      if(method==="POST"&&path==="/api/admin/lock"){
        adminOnly(me);
        const b=await input(request,10000),lock=!!b.lock;
        const upd=await env.DB.prepare("UPDATE users SET locked=? WHERE id=? AND role='member'")
          .bind(lock?1:0,b.user_id).run();
        if(upd.meta?.changes!==1)throw new ApiError(404,"Member not found");
        if(lock)await env.DB.prepare("DELETE FROM sessions WHERE user_id=?").bind(b.user_id).run();
        return json({ok:true});
      }
      if(method==="GET"&&path==="/api/tryons"){
        const list=await env.DB.prepare("SELECT * FROM tryons WHERE user_id=? ORDER BY created_at DESC LIMIT 30")
          .bind(me.id).all();
        return json({jobs:list.results.map(publicJob)});
      }
      if(method==="POST"&&path==="/api/tryon"){
        if(!env.FASHN_API_KEY)throw new ApiError(503,"AI is not configured on the server");
        const b=await input(request,3350000);
        if(!checkImage(b.person_image)||!checkImage(b.garment_image))
          throw new ApiError(400,"Provide JPEG/PNG/WEBP images under 1.2 MB each");
        const count=await env.DB.prepare("SELECT COUNT(*) AS n FROM tryons WHERE user_id=? AND status='processing'")
          .bind(me.id).first();
        if(count.n>=1)throw new ApiError(429,"Finish previous try-on first");
        const deducted=await env.DB.prepare("UPDATE users SET credits=credits-1 WHERE id=? AND credits>0").bind(me.id).run();
        if(deducted.meta?.changes!==1)throw new ApiError(402,"No AI credits: ask ADM to grant credits");
        let pred;
        try{pred=await requestProvider(env,b);}
        catch(e){
          await env.DB.prepare("UPDATE users SET credits=credits+1 WHERE id=?").bind(me.id).run();
          throw e;
        }
        const id=crypto.randomUUID();
        await env.DB.prepare("INSERT INTO tryons(id,user_id,provider_id,status,credits_spent,result_url,created_at,updated_at) VALUES(?,?,?,'processing',1,NULL,?,?)")
          .bind(id,me.id,pred,Date.now(),Date.now()).run();
        return json({job:{id,status:"processing"}},202);
      }
      const match=/^\/api\/tryon\/([a-f0-9-]{36})$/.exec(path);
      if(method==="GET"&&match){
        let job=await env.DB.prepare("SELECT * FROM tryons WHERE id=? AND user_id=?").bind(match[1],me.id).first();
        if(!job)throw new ApiError(404,"Try-on not found");
        try{job=await reconcile(env,job);}catch(e){/* report saved job, do not invent success */}
        return json({job:publicJob(job)});
      }
      throw new ApiError(404,"Not found");
    }catch(e){
      const code=e instanceof ApiError?e.code:500;
      return json({error:code===500?"Internal server error":e.message},code);
    }
  }
};