const assert=require('node:assert/strict');
const fs=require('node:fs'),path=require('node:path'),http=require('node:http');
const {chromium}=require('playwright');

(async()=>{
 const root=path.resolve('www');
 const server=http.createServer((req,res)=>{
  const file=path.resolve(root,'.'+new URL(req.url,'http://local').pathname.replace(/\/$/,'/index.html'));
  if(!file.startsWith(root+path.sep)){res.writeHead(403);res.end();return}
  fs.readFile(file,(e,d)=>{if(e){res.writeHead(404);res.end();return}res.setHeader('Content-Type',file.endsWith('.js')?'text/javascript':file.endsWith('.css')?'text/css':file.endsWith('.html')?'text/html':'text/plain');res.end(d)});
 });
 await new Promise(r=>server.listen(0,'127.0.0.1',r));
 let browser;
 try{
  browser=await chromium.launch({headless:true,executablePath:process.env.TRAINPILOT_CHROMIUM||undefined,args:['--no-sandbox']});
  for(const viewport of [{width:360,height:640},{width:393,height:780}]){
   const page=await browser.newPage({viewport,locale:'hu-HU'});
   await page.goto('http://127.0.0.1:'+server.address().port);
   await page.waitForFunction(()=>window.TrainPilotBoot?.finished);
   await page.waitForTimeout(120);

   const head=await page.evaluate(()=>{
    const strip=document.querySelector('.tp-brand-strip'),top=document.querySelector('.top');
    if(!top)return null;
    const n=top.getBoundingClientRect();
    return {brandPresent:!!strip,n:{top:n.top,bottom:n.bottom}};
   });
   assert.ok(head,'sticky navigation must exist');
   assert.equal(head.brandPresent,false,'Home brand/version strip must be physically removed');
   assert.ok(head.n.top<=1,'primary navigation must be the topmost Home chrome');

   await page.evaluate(()=>profileScreen());
   await page.waitForTimeout(80);
   const acc=await page.evaluate(()=>{
    const ds=[...document.querySelectorAll('.rf148-profile-accordion')];
    return ds.map(d=>{const r=d.getBoundingClientRect(),s=d.querySelector('summary')?.getBoundingClientRect();return {top:r.top,bottom:r.bottom,summaryHeight:s?.height||0,text:d.querySelector('summary')?.textContent.trim()||''}});
   });
   assert.equal(acc.length,2,'profile should have two dedicated exclusion accordions');
   assert.ok(acc[1].top-acc[0].bottom>=12,'closed exclusion accordions need clear visual spacing');
   assert.ok(acc.every(x=>x.summaryHeight>=46),'accordion titles need comfortable touch height');

   await page.evaluate(()=>go('programs'));
   await page.waitForTimeout(80);
   const programText=await page.locator('main').innerText();
   for(const name of ['Otthoni felsőtest A/B','Otthoni gyors 30 perc','Calisthenics A/B','Gumiszalagos A/B','Kettlebell Full Body A/B','Konditermi felső / alsó','Konditermi Push / Pull / Legs','Konditermi erő A/B/C']){
    assert.ok(programText.includes(name),'missing visible preset: '+name);
   }
   await page.close();
  }
  console.log('PASS: 1.4.8 header spacing, profile accordion spacing and expanded preset library.');
 }finally{
  if(browser)await browser.close();
  await new Promise(r=>server.close(r));
 }
})().catch(e=>{console.error(e);process.exit(1)});
