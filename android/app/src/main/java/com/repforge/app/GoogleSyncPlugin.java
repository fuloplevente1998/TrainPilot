package com.repforge.app;

import android.accounts.Account;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import com.getcapacitor.*;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.google.android.gms.auth.api.identity.*;
import com.google.android.gms.common.api.Scope;
import java.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.*;
import org.json.*;

/** No token is exposed to JavaScript, backup files, preferences or logs. */
@CapacitorPlugin(name="GoogleSync")
public class GoogleSyncPlugin extends Plugin {
 private PluginCall pending; private String action; private volatile String token;
 private ActivityResultLauncher<IntentSenderRequest> launcher;
 private static final int LIMIT=20*1024*1024;
 private static final int PHOTO_LIMIT=6*1024*1024;
 private final ExecutorService ioExecutor=Executors.newSingleThreadExecutor(r->{Thread t=new Thread(r,"TrainPilot-GoogleSync");t.setDaemon(true);return t;});
 private SharedPreferences prefs(){return getContext().getSharedPreferences("repforge-google",Context.MODE_PRIVATE);}
 @Override public void load(){
  launcher=getBridge().registerForActivityResult(new ActivityResultContracts.StartIntentSenderForResult(),r->{
   if(pending==null)return;
   if(r.getResultCode()!=Activity.RESULT_OK){fail("A Google-kapcsolódást megszakítottad.");return;}
   try{authorized(Identity.getAuthorizationClient(getActivity()).getAuthorizationResultFromIntent(r.getData()));}
   catch(Exception e){fail("Nem érhető el a Google-engedély. Próbáld újra.");}
  });
 }
 @Override protected void handleOnDestroy(){super.handleOnDestroy();ioExecutor.shutdownNow();}
 @PluginMethod public void status(PluginCall c){JSObject r=new JSObject();r.put("profile",prefs().getString("profile",""));c.resolve(r);}
 @PluginMethod public void connect(PluginCall c){begin(c,"connect");}
 @PluginMethod public void driveList(PluginCall c){begin(c,"driveList");}
 @PluginMethod public void driveRead(PluginCall c){begin(c,"driveRead");}
 @PluginMethod public void driveWrite(PluginCall c){begin(c,"driveWrite");}
 @PluginMethod public void drivePhotoWrite(PluginCall c){begin(c,"drivePhotoWrite");}
 @PluginMethod public void drivePhotoRead(PluginCall c){begin(c,"drivePhotoRead");}
 @PluginMethod public void drivePhotoDelete(PluginCall c){begin(c,"drivePhotoDelete");}
 @PluginMethod public void calendarSync(PluginCall c){begin(c,"calendarSync");}
 @PluginMethod public void calendarPrepare(PluginCall c){begin(c,"calendarPrepare");}
 @PluginMethod public void calendarDeleteManaged(PluginCall c){begin(c,"calendarDeleteManaged");}
 @PluginMethod public void driveDeleteAll(PluginCall c){begin(c,"driveDeleteAll");}
 @PluginMethod public void drivePrune(PluginCall c){begin(c,"drivePrune");}
 private void begin(PluginCall c,String op){
  if(pending!=null){c.reject("Már folyamatban van egy Google-művelet.");return;}
  String email=prefs().getString("email","");if(!op.equals("connect")&&email.isEmpty()){c.reject("Előbb kapcsolódj a Google-fiókodhoz.");return;}
  pending=c;action=op;
  List<Scope> scopes=new ArrayList<>();for(String s:new String[]{"openid","https://www.googleapis.com/auth/userinfo.email","https://www.googleapis.com/auth/userinfo.profile"})scopes.add(new Scope(s));
  if(op.startsWith("drive"))scopes.add(new Scope("https://www.googleapis.com/auth/drive.appdata"));
  if(op.startsWith("calendar")){scopes.add(new Scope("https://www.googleapis.com/auth/calendar.app.created"));scopes.add(new Scope("https://www.googleapis.com/auth/calendar.calendarlist.readonly"));}
  AuthorizationRequest.Builder b=AuthorizationRequest.builder().setRequestedScopes(scopes);if(!email.isEmpty())b.setAccount(new Account(email,"com.google"));
  getActivity().runOnUiThread(()->Identity.getAuthorizationClient(getActivity()).authorize(b.build()).addOnSuccessListener(r->{
   if(r.hasResolution()){
    if(Boolean.TRUE.equals(c.getBoolean("silent",false))){fail("Google-engedély szükséges. Indíts kézi szinkront.");return;}
    try{launcher.launch(new IntentSenderRequest.Builder(r.getPendingIntent().getIntentSender()).build());}catch(Exception e){fail("Nem nyitható meg a Google-engedélykérés.");}
   }else authorized(r);
  }).addOnFailureListener(e->fail("A Google-kapcsolat nem sikerült. Ellenőrizd az internetet és a TrainPilot Google Cloud/OAuth beállítását.")));
 }
 private void authorized(AuthorizationResult r){
  token=r.getAccessToken();if(token==null){fail("A Google nem adott hozzáférést.");return;}
  ioExecutor.submit(()->{try{
   JSONObject profile=request("GET","https://www.googleapis.com/oauth2/v3/userinfo",null);
   if(profile.optString("sub").isEmpty()||profile.optString("email").isEmpty())throw new IOException("Hiányos Google-profil.");
   String owner=prefs().getString("sub","");if(!owner.isEmpty()&&!owner.equals(profile.getString("sub")))throw new IOException("Eltérő Google-fiók. Előbb jelentkezz ki.");
   JSObject out=new JSObject();
   switch(action){
    case "connect":
     JSONObject safe=new JSONObject();safe.put("sub",profile.getString("sub"));safe.put("email",profile.getString("email"));safe.put("name",profile.optString("name"));
     prefs().edit().putString("profile",safe.toString()).putString("email",profile.getString("email")).putString("sub",profile.getString("sub")).apply();out.put("profile",safe.toString());break;
    case "driveList":
     JSONArray all=new JSONArray();String page="";
     do{JSONObject x=request("GET","https://www.googleapis.com/drive/v3/files?spaces=appDataFolder&q="+enc("trashed = false and name contains 'repforge-sync-'")+"&fields="+enc("nextPageToken,files(id,name,createdTime,version)")+"&pageSize=1000"+(page.isEmpty()?"":"&pageToken="+enc(page)),null);
      JSONArray a=x.optJSONArray("files");if(a!=null)for(int i=0;i<a.length();i++)all.put(a.get(i));page=x.optString("nextPageToken");
     }while(!page.isEmpty());out.put("files",all);break;
    case "driveRead":
     String id=pending.getString("id","");if(!id.matches("[A-Za-z0-9_-]+"))throw new IOException("Hibás fájlazonosító.");
     JSONObject meta=request("GET","https://www.googleapis.com/drive/v3/files/"+id+"?fields=name,version",null);
     if(!meta.optString("name").startsWith("repforge-sync-"))throw new IOException("Nem TrainPilot-kompatibilis mentés.");
     out.put("data",request("GET","https://www.googleapis.com/drive/v3/files/"+id+"?alt=media",null).toString());out.put("version",meta.optString("version"));break;
    case "driveWrite":writeSnapshot(out);break;
    case "drivePhotoWrite":writePhoto(out);break;
    case "drivePhotoRead":readPhoto(out);break;
    case "drivePhotoDelete":deletePhoto(out);break;
    case "calendarSync":syncEvents(out);break;
    case "calendarPrepare":out.put("id",calendar());break;
    case "calendarDeleteManaged":deleteManagedCalendar(out);break;
    case "driveDeleteAll":deleteAppData(out);break;
    case "drivePrune":pruneSnapshots(out);break;
    default:throw new IOException("Ismeretlen művelet.");
   }succeed(out);
  }catch(Exception e){
   String lang=pending==null?Locale.getDefault().getLanguage():pending.getString("language",Locale.getDefault().getLanguage());
   String network=GoogleRequestPolicy.networkMessage(e,lang);
   if(network!=null)fail(network,"GOOGLE_NETWORK");
   else fail(e instanceof ApiError?apiMessage((ApiError)e):e.getMessage());
  }});
 }
 private void writeSnapshot(JSObject out)throws Exception{
  String data=pending.getString("data","");JSONObject parsed=new JSONObject(data);
  if(!parsed.optString("app").equals("RepForgeSync")||!parsed.optString("owner").equals(prefs().getString("sub",""))||data.getBytes(StandardCharsets.UTF_8).length>LIMIT)throw new IOException("Érvénytelen felhőmentés.");
  String device=parsed.getString("device");if(!device.matches("[a-z0-9-]{36}"))throw new IOException("Hibás eszközazonosító.");
  JSONObject meta=new JSONObject();meta.put("name","repforge-sync-"+device+"-"+UUID.randomUUID()+".json");meta.put("parents",new JSONArray().put("appDataFolder"));meta.put("mimeType","application/json");
  String boundary="repforge"+UUID.randomUUID().toString().replace("-","");
  String body="--"+boundary+"\r\nContent-Type: application/json; charset=UTF-8\r\n\r\n"+meta+"\r\n--"+boundary+"\r\nContent-Type: application/json\r\n\r\n"+data+"\r\n--"+boundary+"--\r\n";
  JSONObject created=http("POST","https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart&fields=id,version",body,"multipart/related; boundary="+boundary);
  JSONObject read=request("GET","https://www.googleapis.com/drive/v3/files/"+created.getString("id")+"?alt=media",null);
  if(!read.toString().equals(parsed.toString()))throw new IOException("A felhőmentés visszaellenőrzése nem sikerült.");
  out.put("id",created.getString("id"));out.put("version",created.optString("version"));out.put("verified",true);
 }
 private String photoId()throws IOException{String id=pending.getString("photoId","");if(!id.matches("[a-f0-9-]{36}"))throw new IOException("Hibás fotóazonosító.");return id;}
 private File photoFile(String id)throws IOException{File d=new File(getContext().getFilesDir(),"workout_photos");if(!d.exists()&&!d.mkdirs())throw new IOException("A fotómappa nem hozható létre.");return new File(d,id+".jpg");}
 private String photoName(String id){return "trainpilot-photo-"+id+".jpg";}
 private String md5(File f)throws Exception{MessageDigest d=MessageDigest.getInstance("MD5");try(InputStream in=new FileInputStream(f)){byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1)d.update(b,0,n);}StringBuilder s=new StringBuilder();for(byte x:d.digest())s.append(String.format(Locale.US,"%02x",x&255));return s.toString();}
 private void writePhoto(JSObject out)throws Exception{
  String id=photoId(),name=photoName(id);File f=photoFile(id);if(!f.isFile()||f.length()<=0||f.length()>PHOTO_LIMIT)throw new IOException("A helyi fotó hiányzik vagy túl nagy.");
  JSONObject existing=request("GET","https://www.googleapis.com/drive/v3/files?spaces=appDataFolder&q="+enc("trashed = false and name = '"+name+"'")+"&fields="+enc("files(id,name,size,md5Checksum)")+"&pageSize=2",null);
  JSONArray files=existing.optJSONArray("files");if(files!=null&&files.length()>0){JSONObject x=files.getJSONObject(0);if(x.optLong("size",-1)==f.length()&&md5(f).equalsIgnoreCase(x.optString("md5Checksum"))){out.put("id",x.getString("id"));out.put("verified",true);return;}}
  JSONObject meta=new JSONObject();meta.put("name",name);meta.put("parents",new JSONArray().put("appDataFolder"));meta.put("mimeType","image/jpeg");
  String boundary="trainpilot"+UUID.randomUUID().toString().replace("-","");
  HttpURLConnection c=(HttpURLConnection)new URL("https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart&fields=id,name,size,md5Checksum").openConnection();c.setConnectTimeout(20000);c.setReadTimeout(45000);c.setRequestMethod("POST");c.setDoOutput(true);c.setRequestProperty("Authorization","Bearer "+token);c.setRequestProperty("Accept","application/json");c.setRequestProperty("Content-Type","multipart/related; boundary="+boundary);c.setChunkedStreamingMode(8192);
  try(OutputStream o=c.getOutputStream();InputStream in=new FileInputStream(f)){
   o.write(("--"+boundary+"\r\nContent-Type: application/json; charset=UTF-8\r\n\r\n"+meta+"\r\n--"+boundary+"\r\nContent-Type: image/jpeg\r\n\r\n").getBytes(StandardCharsets.UTF_8));byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1)o.write(b,0,n);o.write(("\r\n--"+boundary+"--\r\n").getBytes(StandardCharsets.UTF_8));
  }
  try{int status=c.getResponseCode();if(status<200||status>=300)throw apiError(c,status);JSONObject created=new JSONObject(new String(readResponse(c,LIMIT),StandardCharsets.UTF_8));if(!name.equals(created.optString("name"))||created.optLong("size",-1)!=f.length()||!md5(f).equalsIgnoreCase(created.optString("md5Checksum")))throw new IOException("A fotó Drive-ellenőrzése nem sikerült.");out.put("id",created.getString("id"));out.put("verified",true);}finally{c.disconnect();}
 }
 private void readPhoto(JSObject out)throws Exception{
  String id=pending.getString("id","");if(!id.matches("[A-Za-z0-9_-]+"))throw new IOException("Hibás Drive-fájlazonosító.");String pid=photoId(),name=photoName(pid);JSONObject meta=request("GET","https://www.googleapis.com/drive/v3/files/"+id+"?fields=name,size,mimeType",null);if(!name.equals(meta.optString("name"))||!"image/jpeg".equals(meta.optString("mimeType"))||meta.optLong("size",-1)<0||meta.optLong("size",0)>PHOTO_LIMIT)throw new IOException("Nem érvényes TrainPilot-fotó.");byte[] bytes=download("https://www.googleapis.com/drive/v3/files/"+id+"?alt=media",PHOTO_LIMIT);if(bytes.length!=meta.optLong("size"))throw new IOException("A letöltött fotó mérete eltér.");File f=photoFile(pid),tmp=new File(f.getParentFile(),pid+".tmp");try(FileOutputStream o=new FileOutputStream(tmp)){o.write(bytes);o.getFD().sync();}if(f.exists()&&!f.delete())throw new IOException("A régi helyi fotó nem cserélhető.");if(!tmp.renameTo(f))throw new IOException("A letöltött fotó nem menthető.");out.put("photoId",pid);out.put("bytes",bytes.length);out.put("verified",true);
 }
 private void deletePhoto(JSObject out)throws Exception{
  String id=pending.getString("id","");if(!id.matches("[A-Za-z0-9_-]+"))throw new IOException("Hibás Drive-fájlazonosító.");String pid=photoId(),name=photoName(pid);try{JSONObject meta=request("GET","https://www.googleapis.com/drive/v3/files/"+id+"?fields=name",null);if(!name.equals(meta.optString("name")))throw new IOException("Nem ehhez a naplófotóhoz tartozó Drive-fájl.");request("DELETE","https://www.googleapis.com/drive/v3/files/"+id,null);}catch(ApiError e){if(e.code!=404&&e.code!=410)throw e;}out.put("deleted",true);
 }
 private byte[] download(String url,int limit)throws Exception{
  for(int attempt=0;;attempt++){HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection();c.setInstanceFollowRedirects(false);c.setConnectTimeout(20000);c.setReadTimeout(45000);c.setRequestMethod("GET");c.setRequestProperty("Authorization","Bearer "+token);try{int status=c.getResponseCode();if(status<200||status>=300){ApiError error=apiError(c,status);if(attempt<3&&GoogleRequestPolicy.transientStatus(status,error.reason)){long delay=GoogleRequestPolicy.delay(attempt,c.getHeaderField("Retry-After"),System.currentTimeMillis());if(delay>=0){c.disconnect();Thread.sleep(delay);continue;}}throw error;}return readResponse(c,limit);}finally{c.disconnect();}}
 }
 private byte[] readResponse(HttpURLConnection c,int limit)throws IOException{try(InputStream in=c.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1){if(out.size()+n>limit)throw new IOException("A válasz túl nagy.");out.write(b,0,n);}return out.toByteArray();}}
 private String calendar()throws Exception{
  String id=prefs().getString("calendar","");if(!id.isEmpty()){
   try{JSONObject c=request("GET","https://www.googleapis.com/calendar/v3/calendars/"+enc(id),null);if(!c.optString("description").equals("RepForge managed workout calendar v1"))throw new IOException("This is not a TrainPilot-managed calendar.");return id;}
   catch(ApiError e){if(e.code!=404&&e.code!=410)throw e;prefs().edit().remove("calendar").apply();id="";}
  }
  String page="";do{
   JSONObject r=request("GET","https://www.googleapis.com/calendar/v3/users/me/calendarList?maxResults=250"+(page.isEmpty()?"":"&pageToken="+enc(page)),null);
   JSONArray a=r.optJSONArray("items");if(a!=null)for(int i=0;i<a.length();i++){JSONObject c=a.getJSONObject(i);if(c.optString("description").equals("RepForge managed workout calendar v1")&&c.optString("accessRole").equals("owner"))id=c.getString("id");}page=r.optString("nextPageToken");
  }while(id.isEmpty()&&!page.isEmpty());
  if(id.isEmpty()){JSONObject c=new JSONObject();c.put("summary","TrainPilot edzések");c.put("description","RepForge managed workout calendar v1");c.put("timeZone",TimeZone.getDefault().getID());id=request("POST","https://www.googleapis.com/calendar/v3/calendars",c).getString("id");}
  prefs().edit().putString("calendar",id).apply();return id;
 }
 private void syncEvents(JSObject out)throws Exception{
  JSONArray events=new JSONArray(pending.getString("events","[]"));if(events.length()>100)throw new IOException("Maximum 100 esemény egy kérésben.");
  String selected=calendar(),expected=pending.getString("calendarId","");if(!expected.isEmpty()&&!expected.equals(selected))throw new IOException("Calendar changed during sync. Run sync again to resend every event.");
  String base="https://www.googleapis.com/calendar/v3/calendars/"+enc(selected)+"/events";int count=0;
  for(int i=0;i<events.length();i++){
   JSONObject e=events.getJSONObject(i);String id=e.optString("id");if(!id.matches("rf[0-9a-f]{64}"))throw new IOException("Hibás eseményazonosító.");
   if(e.optBoolean("cancelled")){try{request("DELETE",base+"/"+id,null);}catch(ApiError error){if(error.code!=404&&error.code!=410)throw error;}}
   else{e.remove("attendees");e.remove("organizer");e.remove("creator");try{request("POST",base+"?sendUpdates=none",e);}catch(ApiError error){if(error.code!=409)throw error;request("PUT",base+"/"+id+"?sendUpdates=none",e);}}
   count++;
  }out.put("count",count);
 }
 @PluginMethod public void disconnect(PluginCall c){if(pending!=null){c.reject("Várd meg a folyamatban lévő művelet végét.");return;}token=null;prefs().edit().clear().apply();c.resolve();}
 private String enc(String x)throws Exception{return URLEncoder.encode(x,"UTF-8");}
 private JSONObject request(String method,String url,JSONObject body)throws Exception{return http(method,url,body==null?null:body.toString(),"application/json; charset=UTF-8");}
 private JSONObject http(String method,String url,String body,String type)throws Exception{
  // GET/PUT/DELETE are safe to repeat; Calendar event POST uses deterministic IDs.
  // Drive uploads and calendar creation cannot be retried blindly after an uncertain response.
  boolean retryable=GoogleRequestPolicy.canRetry(method,url,body==null?"":new JSONObject(method.equals("POST")&&type.startsWith("application/json")?body:"{}").optString("id"));
  for(int attempt=0;;attempt++){
   HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection();c.setInstanceFollowRedirects(false);c.setConnectTimeout(20000);c.setReadTimeout(30000);c.setRequestMethod(method);c.setRequestProperty("Authorization","Bearer "+token);c.setRequestProperty("Accept","application/json");
   try{if(body!=null){c.setDoOutput(true);c.setRequestProperty("Content-Type",type);try(OutputStream o=c.getOutputStream()){o.write(body.getBytes(StandardCharsets.UTF_8));}}
    int status=c.getResponseCode();if(status<200||status>=300){
     ApiError error=apiError(c,status);
     if(retryable&&attempt<3&&GoogleRequestPolicy.transientStatus(status,error.reason)){long delay=GoogleRequestPolicy.delay(attempt,c.getHeaderField("Retry-After"),System.currentTimeMillis());if(delay>=0){c.disconnect();Thread.sleep(delay);continue;}}

     if(status==401)Identity.getAuthorizationClient(getContext()).clearToken(ClearTokenRequest.builder().setToken(token).build());throw error;
    }
    if(status==204)return new JSONObject();String text=new String(readResponse(c,LIMIT),StandardCharsets.UTF_8);return text.isEmpty()?new JSONObject():new JSONObject(text);
   }finally{c.disconnect();}
  }
 }
 private JSONArray appFiles(String query)throws Exception{
  JSONArray all=new JSONArray();String page="";do{JSONObject result=request("GET","https://www.googleapis.com/drive/v3/files?spaces=appDataFolder&q="+enc("trashed = false and ("+query+")")+"&fields="+enc("nextPageToken,files(id,name,createdTime)")+"&pageSize=1000"+(page.isEmpty()?"":"&pageToken="+enc(page)),null);JSONArray files=result.optJSONArray("files");if(files!=null)for(int i=0;i<files.length();i++)all.put(files.get(i));page=result.optString("nextPageToken");}while(!page.isEmpty());return all;
 }
 private boolean appFileName(String name){return name.matches("repforge-sync-[a-z0-9-]{36}-[A-Za-z0-9_-]{1,80}\\.json|trainpilot-photo-[a-f0-9-]{36}\\.jpg");}
 private void deleteAppData(JSObject out)throws Exception{
  JSONArray files=appFiles("name contains 'repforge-sync-' or name contains 'trainpilot-photo-'");int count=0;for(int i=0;i<files.length();i++){JSONObject f=files.getJSONObject(i);if(!appFileName(f.optString("name")))continue;request("DELETE","https://www.googleapis.com/drive/v3/files/"+enc(f.getString("id")),null);count++;}out.put("deleted",count);
 }
 private void pruneSnapshots(JSObject out)throws Exception{
  String device=pending.getString("device","");if(!device.matches("[a-f0-9-]{36}"))throw new IOException("Invalid device ID.");JSONArray files=appFiles("name contains 'repforge-sync-"+device+"-'");List<JSONObject> rows=new ArrayList<>();for(int i=0;i<files.length();i++){JSONObject f=files.getJSONObject(i);if(appFileName(f.optString("name")))rows.add(f);}rows.sort(Comparator.comparing(f->f.optString("createdTime")));int deleted=0,protectedLegacy=0;
  // Keep the oldest safety snapshot and the latest ten. Never remove another device's backups.
  for(int i=1;i<rows.size()-10;i++){JSONObject f=rows.get(i),snapshot=request("GET","https://www.googleapis.com/drive/v3/files/"+enc(f.getString("id"))+"?alt=media",null);if(!snapshot.optString("app").equals("RepForgeSync")||!snapshot.optString("owner").equals(prefs().getString("sub",""))||!snapshot.optString("device").equals(device))continue;JSONObject data=snapshot.optJSONObject("data");boolean legacy=data==null;for(String key:new String[]{"history","weights"}){JSONArray a=data==null?null:data.optJSONArray(key);if(a==null){legacy=true;continue;}for(int j=0;j<a.length();j++){String id=a.getJSONObject(j).optString("id");if(id.isEmpty()||id.startsWith("tp18-"))legacy=true;}}if(legacy){protectedLegacy++;continue;}request("DELETE","https://www.googleapis.com/drive/v3/files/"+enc(f.getString("id")),null);deleted++;}
  out.put("deleted",deleted);out.put("protectedLegacy",protectedLegacy);
 }
 private void deleteManagedCalendar(JSObject out)throws Exception{
  String page="";List<String> ids=new ArrayList<>();do{JSONObject r=request("GET","https://www.googleapis.com/calendar/v3/users/me/calendarList?maxResults=250"+(page.isEmpty()?"":"&pageToken="+enc(page)),null);JSONArray a=r.optJSONArray("items");if(a!=null)for(int i=0;i<a.length();i++){JSONObject x=a.getJSONObject(i);if(x.optString("description").equals("RepForge managed workout calendar v1")&&x.optString("accessRole").equals("owner"))ids.add(x.getString("id"));}page=r.optString("nextPageToken");}while(!page.isEmpty());
  for(String id:ids)try{request("DELETE","https://www.googleapis.com/calendar/v3/calendars/"+enc(id),null);}catch(ApiError e){if(e.code!=404&&e.code!=410)throw e;}prefs().edit().remove("calendar").apply();out.put("deleted",ids.size());
 }
 @PluginMethod public void revoke(PluginCall c){
  if(pending!=null){c.reject("Wait for the current Google operation to finish.");return;}String email=prefs().getString("email","");if(email.isEmpty()){c.reject("Connect to Google first.");return;}pending=c;
  List<Scope> scopes=new ArrayList<>();for(String s:new String[]{"openid","https://www.googleapis.com/auth/userinfo.email","https://www.googleapis.com/auth/userinfo.profile","https://www.googleapis.com/auth/drive.appdata","https://www.googleapis.com/auth/calendar.app.created","https://www.googleapis.com/auth/calendar.calendarlist.readonly"})scopes.add(new Scope(s));
  Identity.getAuthorizationClient(getActivity()).revokeAccess(RevokeAccessRequest.builder().setAccount(new Account(email,"com.google")).setScopes(scopes).build()).addOnSuccessListener(v->{prefs().edit().clear().apply();succeed(new JSObject());}).addOnFailureListener(e->fail("Google permission revocation failed. Retry online; access has not been reported as revoked."));
 }
 private ApiError apiError(HttpURLConnection connection,int code){
  String reason="";try(InputStream in=connection.getErrorStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){if(in!=null){byte[] b=new byte[1024];int n;while((n=in.read(b))!=-1&&out.size()+n<=8192)out.write(b,0,n);JSONObject error=new JSONObject(out.toString("UTF-8")).optJSONObject("error");JSONArray details=error==null?null:error.optJSONArray("errors");if(details!=null&&details.length()>0)reason=details.getJSONObject(0).optString("reason");}}
  catch(Exception ignored){}return new ApiError(code,reason);
 }
 private String apiMessage(ApiError error){
  String[] messages;
  if(error.code==401)messages=new String[]{"A Google-engedély lejárt vagy visszavonták. Indíts kézi szinkront az új engedélykéréshez.","Google access expired or was revoked. Run manual sync to request access again.","Google-Zugriff ist abgelaufen oder widerrufen. Manuell synchronisieren, um erneut Zugriff anzufordern.","Accesul Google a expirat sau a fost revocat. Pornește sincronizarea manuală pentru o nouă autorizare."};
  else if(error.reason.equals("storageQuotaExceeded"))messages=new String[]{"A Google Drive tárhelye megtelt. Szabadíts fel helyet, majd indíts kézi szinkront.","Google Drive storage is full. Free some space, then run manual sync.","Google Drive ist voll. Speicher freigeben und manuell synchronisieren.","Spațiul Google Drive este plin. Eliberează spațiu și sincronizează manual."};
  else if(error.code==429||GoogleRequestPolicy.transientStatus(error.code,error.reason))messages=new String[]{"A Google átmenetileg nem fogadja a kérést. Várj, majd próbáld újra; a helyi adatok megmaradnak.","Google is temporarily refusing requests. Wait and retry; local data remains available.","Google lehnt Anfragen vorübergehend ab. Warten und erneut versuchen; lokale Daten bleiben erhalten.","Google refuză temporar cererile. Așteaptă și reîncearcă; datele locale rămân disponibile."};
  else if(error.code==403)messages=new String[]{"A Google nem engedélyezte ezt a műveletet. Ellenőrizd az engedélyt, az API/OAuth beállítást és a tanúsítványt.","Google did not permit this operation. Check permission, API/OAuth configuration and the signing certificate.","Google erlaubt diesen Vorgang nicht. Berechtigung, API/OAuth-Einstellung und Zertifikat prüfen.","Google nu a permis operațiunea. Verifică permisiunea, configurația API/OAuth și certificatul."};
  else messages=new String[]{"A Google-művelet nem sikerült.","The Google operation failed.","Der Google-Vorgang ist fehlgeschlagen.","Operațiunea Google a eșuat."};
  String lang=pending==null?Locale.getDefault().getLanguage():pending.getString("language",Locale.getDefault().getLanguage());return messages[lang.equals("hu")?0:lang.equals("de")?2:lang.equals("ro")?3:1]+" ("+error.code+")";
 }
 private static class ApiError extends IOException{final int code;final String reason;ApiError(int c,String r){code=c;reason=r;}}

 private synchronized void succeed(JSObject out){PluginCall c=pending;pending=null;token=null;if(c!=null)c.resolve(out);}
 private synchronized void fail(String message){PluginCall c=pending;pending=null;token=null;if(c!=null)c.reject(message==null?"Nem sikerült a Google-művelet.":message);}
 private synchronized void fail(String message,String code){PluginCall c=pending;pending=null;token=null;if(c!=null)c.reject(message,code);}
}
