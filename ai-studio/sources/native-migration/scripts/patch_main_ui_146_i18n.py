from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]

app = ROOT / 'www/app.js'
s = app.read_text(encoding='utf-8')
old = r'''var tp146ProgramDayTitle=function tp146ProgramDayTitle(program,day){
 const name=String(program?.name||'Program').replace(/\s*[–—]\s*/g,' - ').replace(/\s+-\s+/g,' - ').trim();
 const d=String(day?.name||day?.id||'').trim();
 return d?`${name} - ${d}`:name;
};'''
new = r'''var tp146ProgramDayTitle=function tp146ProgramDayTitle(program,day){
 const raw=String(program?.name||'Program'),lang=typeof rf212Lang==='function'?rf212Lang():'hu';
 let name=raw;
 if(lang==='en'&&typeof RF213_EN!=='undefined')name=RF213_EN[raw]||raw;
 else if(lang!=='hu'&&typeof RF214_MAPS!=='undefined'){const map=RF214_MAPS[lang]||RF214_MAPS.en||{};name=map[raw]||raw;}
 name=String(name).replace(/\s*[–—]\s*/g,' - ').replace(/\s+-\s+/g,' - ').trim();
 const d=String(day?.name||day?.id||'').trim();
 return d?`${name} - ${d}`:name;
};'''
if old not in s:
    raise SystemExit('tp146ProgramDayTitle marker missing')
s = s.replace(old, new, 1)
app.write_text(s, encoding='utf-8')

testp = ROOT / 'tests/browser/ui-unify-dialogs-2630.cjs'
t = testp.read_text(encoding='utf-8')
old_test = "await page.evaluate(()=>{db.set('activeProgramId','home-basic');state.session=null;state.tab='plan';render()});await page.waitForTimeout(60);\n  const dayTitle=(await page.locator('.tp146-day-title').first().innerText()).replace(/\\s+/g,' ');assert.match(dayTitle,/Otthoni A\\/B - Alap - A/);"
new_test = "await page.evaluate(()=>{db.set('language','hu');db.set('activeProgramId','home-basic');state.session=null;state.tab='plan';render()});await page.waitForTimeout(60);\n  const dayTitle=(await page.locator('.tp146-day-title').first().innerText()).replace(/\\s+/g,' ');assert.match(dayTitle,/Otthoni A\\/B - Alap - A/);\n  await page.evaluate(()=>{db.set('language','en');state.tab='plan';render()});await page.waitForTimeout(40);\n  const enDayTitle=(await page.locator('.tp146-day-title').first().innerText()).replace(/\\s+/g,' ');assert.match(enDayTitle,/Home A\\/B - Basic - A/);assert.ok(!/Homei/.test(enDayTitle),'English title must not partially translate Otthoni');\n  await page.evaluate(()=>{db.set('language','hu');state.tab='plan';render()});await page.waitForTimeout(40);"
if old_test not in t:
    raise SystemExit('ui-unify language test marker missing')
t = t.replace(old_test, new_test, 1)
testp.write_text(t, encoding='utf-8')

print('Applied locale-safe 1.4.6 program/day titles')
