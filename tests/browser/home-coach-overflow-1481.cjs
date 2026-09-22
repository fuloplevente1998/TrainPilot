const assert=require('node:assert/strict');
const fs=require('node:fs'),path=require('node:path'),http=require('node:http');
const {chromium}=require('playwright');

const root=path.resolve('www');
const server=http.createServer((req,res)=>{
 let p=new URL(req.url,'http://local').pathname;if(p==='/')p='/index.html';
 const file=path.resolve(root,'.'+p);if(!file.startsWith(root+path.sep)){res.writeHead(403);res.end();return}
 fs.readFile(file,(e,d)=>{if(e){res.writeHead(404);res.end();return}
  res.setHeader('Content-Type',file.endsWith('.js')?'text/javascript':file.endsWith('.css')?'text/css':file.endsWith('.html')?'text/html':'text/plain');res.end(d);
 });
});
const listen=()=>new Promise(r=>server.listen(0,'127.0.0.1',r));
const base=()=>'http://127.0.0.1:'+server.address().port+'/';

(async()=>{
 await listen();let browser;
 try{
  browser=await chromium.launch({headless:true,executablePath:process.env.TRAINPILOT_CHROMIUM||undefined,args:['--no-sandbox']});
  const page=await browser.newPage({viewport:{width:393,height:873},locale:'hu-HU'});
  page.on('dialog',d=>d.accept());
  await page.goto(base());await page.waitForFunction(()=>window.TrainPilotBoot?.finished);
  await page.evaluate(()=>{state.tab='home';render()});
  await page.waitForFunction(()=>document.querySelector('#rf220CoachCard.tp166-home-coach')&&document.querySelector('#rf220CoachCard .tp166-home-coach-copy p'));

  await page.evaluate(()=>{
   const line=document.querySelector('#rf220CoachCard .tp166-home-coach-copy p');
   line.textContent='Visszafogott nap javasolt • A legutóbbi naplóban fájdalomjelzés szerepel. Az érintett gyakorlatnál ne emelj terhelést. Több fáradtsági jel látszik. Ma ne erőltesd a progressziót; könnyített edzés vagy pihenő lehet célszerű.';
  });

  const layout=await page.evaluate(()=>{
   const main=document.querySelector('main.rf221-home'),coach=document.querySelector('#rf220CoachCard');
   const bodyStyle=getComputedStyle(document.body),mainStyle=getComputedStyle(main),lineStyle=getComputedStyle(document.querySelector('#rf220CoachCard .tp166-home-coach-copy p'));
   const r=coach.getBoundingClientRect();
   const line=document.querySelector('#rf220CoachCard .tp166-home-coach-copy p'),lr=line.getBoundingClientRect(),lh=parseFloat(lineStyle.lineHeight)||0;
   return {bodyOverflowY:bodyStyle.overflowY,mainOverflow:mainStyle.overflow,mainHeight:mainStyle.height,mainPaddingBottom:parseFloat(mainStyle.paddingBottom)||0,lineWhiteSpace:lineStyle.whiteSpace,lineClamp:lineStyle.webkitLineClamp,lineHeight:lh,lineClientHeight:line.clientHeight,lineScrollHeight:line.scrollHeight,coachHeight:r.height,coachTop:r.top,coachBottom:r.bottom,scrollHeight:document.documentElement.scrollHeight,viewport:innerHeight};
  });
  assert.notEqual(layout.bodyOverflowY,'hidden','Home must not lock vertical scrolling');
  assert.notEqual(layout.mainOverflow,'hidden','Home main must not clip dynamic cards');
  assert.equal(layout.lineWhiteSpace,'normal','Coach recommendation must wrap instead of ellipsis clipping');
  assert.ok(layout.scrollHeight>=layout.viewport,'document must remain scrollable when content exceeds viewport');

  await page.locator('#rf220CoachCard').scrollIntoViewIfNeeded();
  await page.waitForTimeout(50);
  const visible=await page.evaluate(()=>{
   const card=document.querySelector('#rf220CoachCard'),buttons=[...card.querySelectorAll('button')];
   const cr=card.getBoundingClientRect();
   return {cardTop:cr.top,cardBottom:cr.bottom,cardLeft:cr.left,cardRight:cr.right,viewport:innerHeight,viewportWidth:innerWidth,role:card.getAttribute('role'),buttons:buttons.length};
  });
  assert.ok(visible.cardTop<visible.viewport&&visible.cardBottom>0,'Coach card must be reachable in viewport');
  assert.equal(visible.role,'button','#13 Home Coach must remain one accessible full-card action');
  assert.equal(visible.buttons,0,'#13 Home Coach must not restore separate action buttons');
  assert.ok(visible.cardLeft>=0&&visible.cardRight<=visible.viewportWidth+1,'Coach card must not overflow horizontally');

  await page.evaluate(()=>{db.set('language','en');document.documentElement.lang='en';state.tab='home';render()});
  await page.waitForFunction(()=>document.querySelector('#rf220CoachCard.tp166-home-coach')&&document.documentElement.lang==='en');
  const enLayout=await page.evaluate(()=>{
   const main=document.querySelector('main.rf221-home'),coach=document.querySelector('#rf220CoachCard'),ms=getComputedStyle(main);
   return {paddingBottom:parseFloat(ms.paddingBottom)||0,scrollHeight:document.documentElement.scrollHeight,viewport:innerHeight,coachText:coach.textContent||''};
  });
  assert.ok(enLayout.paddingBottom>=12&&enLayout.paddingBottom<=24,'English Home must keep only compact safe-area bottom spacing');
  assert.ok(enLayout.scrollHeight>0,'English Home layout height must remain valid without forced viewport padding');
  assert.match(enLayout.coachText,/Coach|readiness|recommendation|Today/i,'English Coach content missing');
  assert.doesNotMatch(enLayout.coachText,/Details|Statistics/i,'#13 must not restore English Home action labels');
  await page.locator('#rf220CoachCard').scrollIntoViewIfNeeded();
  await page.waitForTimeout(50);
  assert.equal(await page.locator('#rf220CoachCard button').count(),0,'English #13 Home Coach must remain full-card only');
  assert.equal(await page.locator('#rf220CoachCard').getAttribute('role'),'button');

  await page.close();
  console.log('PASS: #13 Home Coach stays scrollable, full-card clickable and reachable on 393x873 in Hungarian and English.');
 }finally{if(browser)await browser.close();await new Promise(r=>server.close(r))}
})().catch(e=>{console.error(e);process.exit(1)});
