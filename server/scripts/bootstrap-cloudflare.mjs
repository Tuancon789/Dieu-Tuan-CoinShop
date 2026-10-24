// Runs inside GitHub Actions after a Cloudflare account has been connected by owner secrets.
// Migrates the retired TikXu Worker endpoint to TryStyle; leaves legacy D1/KV untouched.
import fs from "node:fs";
import os from "node:os";
import path from "node:path";
import { fileURLToPath } from "node:url";
const account=process.env.CLOUDFLARE_ACCOUNT_ID;
const token=process.env.CLOUDFLARE_API_TOKEN;
const adminPassword=process.env.ADMIN_BOOTSTRAP_PASSWORD;
if(!/^[0-9a-fA-F]{32}$/.test(account||""))throw Error("Missing/invalid CLOUDFLARE_ACCOUNT_ID GitHub secret");
if(!token||token.length<20)throw Error("Missing CLOUDFLARE_API_TOKEN GitHub secret");
if(typeof adminPassword!=="string"||adminPassword.length<10||adminPassword.length>128)
 throw Error("Set a private ADMIN_BOOTSTRAP_PASSWORD GitHub secret (10–128 chars)");
const root="https://api.cloudflare.com/client/v4/accounts/"+account;
async function cf(endpoint,method="GET",body){
 const resp=await fetch(root+endpoint,{
   method,headers:{"authorization":"Bearer "+token,"content-type":"application/json"},
   ...(body?{body:JSON.stringify(body)}:{})
 });
 const j=await resp.json().catch(()=>({}));
 if(!resp.ok||!j.success)throw Error("Cloudflare API "+method+" "+endpoint+" failed: "+
   (j.errors||[]).map(x=>x.message).join("; ").slice(0,350));
 return j.result;
}
const name="trystyle-ai";
const existing=await cf("/d1/database?name="+encodeURIComponent(name)+"&per_page=100");
let database=Array.isArray(existing)?existing.find(item=>item.name===name):null;
if(!database){
 database=await cf("/d1/database","POST",{name});
 console.log("Created D1 database for TryStyle AI");
}else console.log("Using existing TryStyle AI D1 database");
if(!/^[0-9a-fA-F-]{36}$/.test(database?.uuid||""))throw Error("Cloudflare D1 database ID missing");
const wranglerPath=path.resolve(path.dirname(fileURLToPath(import.meta.url)),"../wrangler.toml");
let config=fs.readFileSync(wranglerPath,"utf8");
let foundId=false;
config=config.split("\n").map(line=>{
 if(line.trim().startsWith("database_id =")){
  foundId=true;return 'database_id = "'+database.uuid+'"';
 }
 return line;
}).join("\n");
if(!foundId)throw Error("Missing D1 database_id in Wrangler config");
fs.writeFileSync(wranglerPath,config);
const subdomainInfo=await cf("/workers/subdomain");
const subdomain=subdomainInfo?.subdomain;
if(!/^[a-z0-9-]+$/i.test(subdomain||""))
 throw Error("Cloudflare Workers subdomain not configured. Activate workers.dev in your account.");
// Explicitly require the original TikXu Cloudflare account before replacing its Worker.
if(subdomain!=="tukuba41007")
 throw Error("Cloudflare account subdomain differs from legacy TikXu (tukuba41007); refusing to replace an unrelated Worker");
const apiUrl="https://dieu-tuan-tikxu-api."+subdomain+".workers.dev";
const secretsFile=path.join(os.tmpdir(),"trystyle-worker-secrets-"+process.pid+".json");
const secrets={ADMIN_BOOTSTRAP_PASSWORD:adminPassword};
if(process.env.FASHN_API_KEY)secrets.FASHN_API_KEY=process.env.FASHN_API_KEY;
fs.writeFileSync(secretsFile,JSON.stringify(secrets),{mode:0o600});
const githubEnv=process.env.GITHUB_ENV;
if(!githubEnv)throw Error("Only intended for GitHub Actions");
fs.appendFileSync(githubEnv,"TRYSTYLE_API_URL="+apiUrl+"\n");
fs.appendFileSync(githubEnv,"TRYSTYLE_SECRET_FILE="+secretsFile+"\n");
console.log("Prepared server deployment for "+apiUrl);
