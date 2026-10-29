'use strict';
// PR277 phase 2: Workout day previews must be derived from real prescriptions,
// without replacing the accepted day editors, video actions or workout start.
const assert=require('node:assert/strict');
const fs=require('node:fs'),path=require('node:path'),http=require('node:http');
const {chromium}=require('playwright');
const root=path.resolve('www'),html=fs.readFileSync(path.join(root,'index.html'),'utf8');
assert.match(html,/tp277DayOverview/,'Day preview renderer must be available in shipped source');
const server=http.createServer((req,res)=>{
 let p=new URL(req.url,'http://local').pathname;if(p==='/')p='/index.html';
 const f=path.resolve(root,'.'+p);
 if(!f.startsWith(root+path.sep)){res.writeHead(403);return res.end()}
 fs.readFile(f,(e,d)=>{if(e){res.writeHead(404);return res.end()}
  res.setHeader('Content-Type',f.endsWith('.js')?'text/javascript':f.endsWith('.css')?'text/css':f.endsWith('.html')?'text/html':'text/plain');res.end(d);
 });
});
(async()=>{
 await new Promise(r=>server.listen(0,'127.0.0.1',r));
 let b;
 try{
  b=await chromium.launch({headless:true,executablePath:process.env.TRAINPILOT_CHROMIUM||undefined,args:['--no-sandbox']});
  for(const width of [320,360,393]){
   const p=await b.newPage({viewport:{width,height:873},locale:'hu-HU'});
   const errors=[];p.on('pageerror',e=>errors.push(e.message));p.on('dialog',d=>d.accept());
   await p.addInitScript(()=>{localStorage.setItem('repforge:onboarding128',JSON.stringify('skipped'))});
   await p.goto('http://127.0.0.1:'+server.address().port+'/');
   await p.waitForFunction(()=>window.TrainPilotBoot?.finished);
   for(const theme of ['classicBlue','blue']){
    await p.evaluate(t=>{rf200SetTheme(t);go('plan')},theme);
    await p.waitForSelector('main.tp152-plan .tp152-day');
    const overview=await p.evaluate(()=>{
     const main=document.querySelector('main.tp152-plan'),p=activeProgram(),day=p?.days?.[0],card=main.querySelector('.tp152-day');
     const expected=(day?.exercises||[]).reduce((n,id)=>{const e=(typeof tp128ProgramExercise==='function'&&tp128ProgramExercise(p,id))||byId(id);return n+Number(tp152Rx(p,id,e).sets||0)},0);
     return {days:main.querySelectorAll('.tp152-day').length,cardCount:main.querySelectorAll('.tp277-day-overview').length,
      first:card.querySelector('.tp277-day-overview')?.textContent,expected,preview:card.querySelector('.tp277-day-preview')?.textContent,
      start:card.querySelector('.tp152-start-day')?.getAttribute('onclick'),active:!!state.session,
      videoExpected:(day?.exercises||[]).filter(id=>!!rf260VideoButton(id)).length,
      pageOverflow:document.documentElement.scrollWidth-document.documentElement.clientWidth};
    });
    assert.ok(overview.days>0,width+'/'+theme+': active program days');
    assert.equal(overview.cardCount,overview.days,width+'/'+theme+': compact summary on every day');
    assert.match(overview.first,new RegExp(String(overview.expected)+' '),width+'/'+theme+': work sets reflect actual prescriptions');
    assert.ok(overview.preview,width+'/'+theme+': real exercise-name preview present');
    assert.match(overview.start,/startWorkout/,width+'/'+theme+': original workout-start action retained');
    assert.equal(overview.active,false,'opening Workout must not silently start a session');
    assert.ok(overview.pageOverflow<=2,width+'/'+theme+': no horizontal overflow');
    const first=p.locator('main.tp152-plan .tp152-day').first();
    await first.locator(':scope > summary .tp152-day-title-copy').click();
    assert.ok(await first.getAttribute('open')!==null,'day must remain expandable');
    const videos=await first.locator('.tp152-day-body .tp-play-btn').count();
    assert.equal(videos,overview.videoExpected,'existing exercise demonstration controls remain in detail');
    assert.ok(await first.locator('.tp152-start-day').isVisible(),'existing start button remains accessible');
    await first.locator(':scope > summary .tp152-day-title-copy').click();
   }
   // Actual phone start through the original control; no fake second action.
   await p.evaluate(()=>{state.session=null;db.set('draft',null);go('plan')});
   const firstDay=p.locator('main.tp152-plan .tp152-day').first();
   await firstDay.locator(':scope > summary .tp152-day-title-copy').click();
   await firstDay.locator('.tp152-start-day').click();
   await p.waitForFunction(()=>!!state.session);
   assert.ok(await p.evaluate(()=>!!db.get('draft',null)?.session),width+': workout start must persist draft');
   assert.ok(await p.locator('.tp153-set-row .check').count()>0,width+': existing set controls must remain');
   assert.deepEqual(errors,[],'no JS errors '+width);
   await p.close();
  }
  console.log('PASS PR277 Workout summaries at 320/360/393 px with two themes and video/start preserved');
 }finally{await b?.close();await new Promise(r=>server.close(r))}
})().catch(e=>{console.error(e);process.exitCode=1});
