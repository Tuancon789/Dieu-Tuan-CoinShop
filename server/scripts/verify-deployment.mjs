// Fail deployment if API is not live and the seeded ADM account cannot sign in.
const url=process.env.TRYSTYLE_API_URL;
const password=process.env.ADMIN_BOOTSTRAP_PASSWORD;
if(!url||!password)throw Error("Missing deployment environment");
const sleep=(ms)=>new Promise(r=>setTimeout(r,ms));
let lastError="Not yet propagated";
for(let attempt=0;attempt<12;attempt++){
 try{
  const health=await fetch(url+"/api/health",{signal:AbortSignal.timeout(10000)});
  const h=await health.json();
  if(!health.ok||!h.ok||h.service!=="TryStyle AI")throw Error("Health response is not TryStyle AI");
  const response=await fetch(url+"/api/login",{
   method:"POST",headers:{"content-type":"application/json"},
   body:JSON.stringify({username:"ADM",password}),signal:AbortSignal.timeout(15000)
  });
  const login=await response.json();
  if(!response.ok||login.user?.role!=="admin"||!login.token)
   throw Error("ADM bootstrap/login unsuccessful (HTTP "+response.status+")");
  await fetch(url+"/api/logout",{
   method:"POST",headers:{"authorization":"Bearer "+login.token},
   signal:AbortSignal.timeout(10000)
  });
  console.log("Server healthy and ADM account provisioned on D1.");
  process.exit(0);
 }catch(error){
  lastError=error.message;
  if(attempt<11)await sleep(4000);
 }
}
throw Error("Deployment verification failed: "+lastError);
