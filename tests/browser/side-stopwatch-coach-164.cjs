const assert=require('node:assert/strict');
const fs=require('node:fs'),path=require('node:path'),http=require('node:http');
const {chromium}=require('playwright');
const root=path.resolve('www');
const server=http.createServer((req,res)=>{let p=new URL(req.url,'http://local').pathname;if(p==='/')p='/index.html';const file=path.resolve(root,'.'+p);if(!file.startsWith(root+path.sep)){res.writeHead(403);res.end();return}fs.readFile(file,(e,d)=>{if(e){res.writeHead(404);res.end();return}res.setHeader('Content-Type',file.endsWith('.js')?'text/javascript':file.endsWith('.css')?'text/css':file.endsWith('.html')?'text/html':'text/plain');res.end(d)})});
const listen=()=>new Promise(r=>server.listen(0,'127.0.0.1',r)),base=()=>'http://127.0.0.1:'+server.address().port+'/';
(async()=>{await listen();let browser;try{
 browser=await chromium.launch({headless:true,executablePath:process.env.TRAINPILOT_CHROMIUM||undefined,args:['--no-sandbox']});
 const page=await browser.newPage({viewport:{width:393,height:873},locale:'hu-HU'});page.on('dialog',d=>d.accept());
 await page.goto(base());await page.waitForFunction(()=>window.TrainPilotBoot?.finished);
 const data=await page.evaluate(()=>{
  const now=new Date().toISOString(),e=byId('side-plank');if(!e)throw Error('side-plank missing');
  const workout={programId:'home-level2',programName:'Otthoni A/B – Haladó',dayId:'A',workout:'A',started:now,finished:now,exercises:[{id:'side-plank',hu:e.hu,en:e.en,loadType:e.loadType,repUnit:e.repUnit,effort:'good',sets:[{set:1,weight:0,reps:'16',leftSeconds:20,rightSeconds:16,done:true},{set:2,weight:0,reps:'18',leftSeconds:22,rightSeconds:18,done:true}]}]};
  db.set('history',[workout]);
  const rec=rf152Recommendation('side-plank'),stats=rf220ExerciseStats('side-plank'),decision=rf233Decision(rf220Readiness()),advice=rf233ExerciseAdvice('side-plank',decision),short=tp155CoachShortAdvice('side-plank',decision,advice),progress=rf220ProgressHtml();
  return {rec,stats,advice,short,progress,journal:formatSet(workout.exercises[0],workout.exercises[0].sets[0])};
 });
 assert.equal(data.rec.perSide,true);assert.match(data.rec.text,/Bal oldal 22 mp/);assert.match(data.rec.text,/Jobb oldal 18 mp/);assert.doesNotMatch(data.rec.text,/ismétlés/i);
 assert.equal(data.stats.perSide,true);assert.equal(data.stats.bestLeft,22);assert.equal(data.stats.bestRight,18);assert.equal(data.stats.bestBalanced,18);
 assert.match(data.advice.text,/Bal oldal 22 mp/);assert.match(data.advice.text,/Jobb oldal 18 mp/);assert.doesNotMatch(data.advice.text,/ismétlés/i);
 assert.match(data.short.text,/Bal oldal 22 mp/);assert.match(data.short.text,/Jobb oldal 18 mp/);assert.doesNotMatch(data.short.text,/\+1.?2.*ismétlés/i);
 assert.match(data.progress,/Bal rekord: 22 mp/);assert.match(data.progress,/Jobb rekord: 18 mp/);
 assert.match(data.journal,/Bal 20 mp/);assert.match(data.journal,/Jobb 16 mp/);
 await page.evaluate(()=>rf220Exercise('side-plank'));await page.waitForTimeout(30);
 const detail=await page.locator('main').innerText();assert.match(detail,/Bal oldal/);assert.match(detail,/Jobb oldal/);assert.match(detail,/22 mp/);assert.match(detail,/18 mp/);assert.doesNotMatch(detail,/Becsült 1RM/);
 await page.close();console.log('PASS: #12 Coach keeps bilateral side-plank time data, stats and time-based advice.');
}finally{if(browser)await browser.close();await new Promise(r=>server.close(r))}})().catch(e=>{console.error(e);process.exit(1)});
