const assert=require('node:assert/strict');
const fs=require('node:fs'),path=require('node:path'),os=require('node:os'),http=require('node:http'),cp=require('node:child_process');
const {chromium}=require('playwright');

const BASE='60c9906f1127a6a86daa97b4d2379e070b025139';
// Archival pre-1.6 parity baseline. The public TrainPilot history starts at 1.6.0,
// so this standalone legacy audit must skip cleanly when that private-history object is absent.
try{cp.execFileSync('git',['cat-file','-e',BASE+'^{commit}'],{stdio:'ignore'});}catch(_){
 console.log('SKIP: legacy 1.4.7 parity baseline is outside the public TrainPilot Git history.');
 process.exit(0);
}

function materializeBaseline(){
 const dir=fs.mkdtempSync(path.join(os.tmpdir(),'tp147-'));
 const files=cp.execFileSync('git',['ls-tree','-r','--name-only',BASE,'www'],{encoding:'utf8'}).trim().split(/\r?\n/).filter(Boolean);
 for(const file of files){
  const out=path.join(dir,file);
  fs.mkdirSync(path.dirname(out),{recursive:true});
  fs.writeFileSync(out,cp.execFileSync('git',['show',BASE+':'+file]));
 }
 return {dir,root:path.join(dir,'www')};
}
function serve(root){
 const server=http.createServer((req,res)=>{
  let p=new URL(req.url,'http://local').pathname;
  if(p==='/')p='/index.html';
  const file=path.resolve(root,'.'+p);
  if(!file.startsWith(root+path.sep)){res.writeHead(403);res.end();return}
  fs.readFile(file,(e,d)=>{
   if(e){res.writeHead(404);res.end();return}
   res.setHeader('Content-Type',file.endsWith('.js')?'text/javascript':file.endsWith('.css')?'text/css':file.endsWith('.html')?'text/html':file.endsWith('.json')?'application/json':'application/octet-stream');
   res.end(d);
  });
 });
 return new Promise(resolve=>server.listen(0,'127.0.0.1',()=>resolve(server)));
}
const url=s=>'http://127.0.0.1:'+s.address().port+'/';
const norm=s=>String(s).replace(/tp-select-menu-\d+-[a-z0-9]+/gi,'tp-select-menu-X').replace(/\s+/g,' ').trim();

async function boot(page,u){
 await page.goto(u,{waitUntil:'load'});
 await page.waitForFunction(()=>window.TrainPilotBoot?.finished);
 await page.waitForTimeout(80);
}
async function routeHtml(page,route){
 await page.evaluate(r=>go(r),route);
 await page.waitForTimeout(30);
 return norm(await page.locator('#app').innerHTML());
}
async function geometryAudit(page,label){
 const out=await page.evaluate((label)=>{
  const visible=e=>{
   const s=getComputedStyle(e),r=e.getBoundingClientRect();
   return s.display!=='none'&&s.visibility!=='hidden'&&Number(s.opacity)!==0&&r.width>0&&r.height>0;
  };
  const doc=document.documentElement;
  const offenders=[];
  const selectors=['.hero','.card','.program-card','.exercise','.setting','.history','.tp146-day-head','.tp146-day-title','.tp146-day-letter','.tabs','.top','.rf148-profile-accordion'];
  for(const e of document.querySelectorAll(selectors.join(','))){
   if(!visible(e))continue;
   if(e.scrollWidth>e.clientWidth+3)offenders.push({kind:'horizontal-overflow',sel:e.className,text:(e.textContent||'').trim().slice(0,120),scroll:e.scrollWidth,client:e.clientWidth});
  }
  for(const e of document.querySelectorAll('.tp146-day-title')){
   if(!visible(e))continue;
   const badge=e.querySelector('.tp146-day-letter');
   if(badge){
    const t=e.getBoundingClientRect(),b=badge.getBoundingClientRect();
    if(b.left<t.left-1||b.right>t.right+1||b.top<t.top-1||b.bottom>t.bottom+1)offenders.push({kind:'day-badge-outside-title',text:e.textContent.trim()});
    if((badge.textContent||'').trim().length>2)offenders.push({kind:'day-badge-too-long',text:badge.textContent.trim()});
   }
  }
  return {label,viewport:{w:innerWidth,h:innerHeight},docOverflow:doc.scrollWidth-innerWidth,offenders};
 },label);
 assert.ok(out.docOverflow<=3,label+' has page-level horizontal overflow: '+JSON.stringify(out));
 assert.deepEqual(out.offenders,[],label+' layout offenders: '+JSON.stringify(out.offenders,null,2));
}

(async()=>{
 const baseline=materializeBaseline();
 const currentRoot=path.resolve('www');
 const [baseServer,curServer]=await Promise.all([serve(baseline.root),serve(currentRoot)]);
 let browser;
 try{
  const baseCss=cp.execFileSync('git',['show',BASE+':www/styles.css']);
  assert.deepEqual(fs.readFileSync('www/styles.css'),baseCss,'stable 1.4.7 styles.css must remain byte-identical');

  browser=await chromium.launch({headless:true,executablePath:process.env.TRAINPILOT_CHROMIUM||undefined,args:['--no-sandbox']});
  const viewports=[{width:320,height:640},{width:360,height:800},{width:393,height:873},{width:412,height:915},{width:640,height:360}];

  for(const viewport of viewports){
   const b=await browser.newPage({viewport,locale:'hu-HU'});
   const c=await browser.newPage({viewport,locale:'hu-HU'});
   b.on('dialog',d=>d.dismiss());c.on('dialog',d=>d.dismiss());
   await boot(b,url(baseServer));await boot(c,url(curServer));

   for(const route of ['home','plan','history','settings']){
    const pair=await Promise.all([routeHtml(b,route),routeHtml(c,route)]);
    assert.equal(pair[1],pair[0],'1.4.7 GUI parity changed on '+route+' at '+viewport.width+'x'+viewport.height);
    await geometryAudit(c,'stable:'+route+':'+viewport.width+'x'+viewport.height);
   }

   await c.evaluate(()=>go('calendar'));await c.waitForTimeout(30);
   await geometryAudit(c,'calendar:'+viewport.width+'x'+viewport.height);

   await c.evaluate(()=>go('programs'));await c.waitForTimeout(30);
   await geometryAudit(c,'programs:'+viewport.width+'x'+viewport.height);

   await c.evaluate(()=>profileScreen());await c.waitForTimeout(30);
   await geometryAudit(c,'profile:'+viewport.width+'x'+viewport.height);
   const warningTone=await c.evaluate(()=>{
    const a=document.querySelector('.rf148-profile-accordion'),s=a&&getComputedStyle(a),q=a?.querySelector('summary'),qs=q&&getComputedStyle(q);
    return s&&qs?{bg:s.backgroundColor,border:s.borderTopColor,title:qs.color,count:document.querySelectorAll('.rf148-profile-accordion').length}:null;
   });
   assert.ok(warningTone&&warningTone.count===2,'both exclusion sections must use the warning accordion style');
   const nums=(warningTone.title.match(/\d*\.?\d+/g)||[]).map(Number);
   assert.ok(nums.length>=3&&nums[0]>nums[1],'exclusion title should have a red-dominant warning tint: '+warningTone.title);

   await c.evaluate(()=>customExerciseScreen());await c.waitForTimeout(30);
   await geometryAudit(c,'custom-exercise:'+viewport.width+'x'+viewport.height);

   await c.evaluate(()=>rf203ExerciseLibrary());await c.waitForTimeout(30);
   await geometryAudit(c,'exercise-library:'+viewport.width+'x'+viewport.height);

   const presetIds=['home-upper-ab','home-quick-30','calisthenics-ab','bands-ab','kettlebell-ab','gym-upper-lower','gym-ppl','gym-strength-abc'];
   for(const id of presetIds){
    await c.evaluate(id=>{db.set('activeProgramId',id);state.tab='plan';render();},id);
    await c.waitForTimeout(20);
    await geometryAudit(c,'preset:'+id+':'+viewport.width+'x'+viewport.height);
    const badgeTexts=await c.locator('.tp146-day-letter').allTextContents();
    assert.ok(badgeTexts.every(x=>x.trim().length<=2),'compact day badge contains long text in '+id+': '+badgeTexts.join('|'));
   }

   await c.evaluate(()=>{
    const p={age:28,height:178,weight:80,goal:'fitness',experience:'intermediate',activity:'mixed',minutes:60,location:'home',gear:['dumbbells','support'],cadence:'alternate',split:'auto',excluded:[],focus:'upper',avoidAreas:['legs']};
    const g=generatePersonalProgram(p);
    db.set('programs',[...programs().filter(x=>x.id!==g.id),g]);
    db.set('activeProgramId',g.id);
    state.tab='plan';render();
   });
   await c.waitForTimeout(30);
   await geometryAudit(c,'generated-upper-ab:'+viewport.width+'x'+viewport.height);
   const generated=await c.evaluate(()=>({
    title:document.querySelector('.hero h1')?.textContent.trim(),
    badges:[...document.querySelectorAll('.tp146-day-letter')].map(e=>e.textContent.trim()),
    dayTitles:[...document.querySelectorAll('.tp146-day-title')].map(e=>e.textContent.trim())
   }));
   assert.ok(/Felsőtest A\/B/.test(generated.title||''),'expected generated upper-body A/B plan');
   assert.deepEqual(generated.badges.slice(0,2),['A','B'],'generated A/B compact badges must stay A/B, never full day names');

   await b.close();await c.close();
  }
  console.log('PASS: 1.4.8 full GUI audit - stable 1.4.7 routes unchanged, no horizontal text/layout collisions across tested sizes, all new presets/exercises fit, generated A/B badge regression fixed.');
 }finally{
  if(browser)await browser.close();
  await Promise.all([new Promise(r=>baseServer.close(r)),new Promise(r=>curServer.close(r))]);
  fs.rmSync(baseline.dir,{recursive:true,force:true});
 }
})().catch(e=>{console.error(e);process.exit(1)});
