const fs=require('fs'),assert=require('node:assert/strict');
const v260=fs.readFileSync('www/v260.js','utf8');
const v200=fs.readFileSync('www/v200.js','utf8');
const sw=fs.readFileSync('www/sw.js','utf8');

for(const marker of [
  'overflow-x:clip!important',
  'env(safe-area-inset-top,0px)',
  'env(safe-area-inset-bottom,0px)',
  'rf260VisualViewport',
  "dataset.placement=openTop?'top':'bottom'",
  'rf260RepositionOpenSelects',
  'rf260OptionSignature',
  'rf260BuildSelectOptions',
  'rf260TrapModalFocus',
  'rf260MoveReturnFocus',
  '@media(max-width:360px)',
  'tp-brand-strip'
]) assert.ok(v260.includes(marker),'missing responsive 2.6.2 marker: '+marker);

assert.ok(sw.includes("trainpilot-v112"),'service worker cache must be 1.1.2');

// All current app dropdowns that use the shared field class are covered by the enhancer.
let selectCount=0;
for(const f of fs.readdirSync('www').filter(x=>/\.(?:js|html)$/.test(x))){
  const s=fs.readFileSync('www/'+f,'utf8');
  selectCount+=(s.match(/<select[^>]*class=["'][^"']*\bfield\b[^"']*["']/g)||[]).length;
}
assert.equal(selectCount,29,'2.6.2 should expose 29 select.field dropdowns including the theme selector');

function hexLum(h){
  const rgb=[1,3,5].map(i=>parseInt(h.slice(i,i+2),16)/255).map(c=>c<=.04045?c/12.92:Math.pow((c+.055)/1.055,2.4));
  return .2126*rgb[0]+.7152*rgb[1]+.0722*rgb[2];
}
function contrast(a,b){const A=hexLum(a),B=hexLum(b),hi=Math.max(A,B),lo=Math.min(A,B);return (hi+.05)/(lo+.05)}
const themes=[...v200.matchAll(/([a-z]+):\{name:'[^']+',accent:'(#[0-9a-fA-F]{6})',accent2:'(#[0-9a-fA-F]{6})'\}/g)];
assert.equal(themes.length,16,'all sixteen themes must be found');
for(const [,name,accent,accent2] of themes){
  assert.ok(contrast(accent,'#171717')>=4.5,`${name} primary accent needs readable dark button text`);
  assert.ok(contrast(accent2,'#1a1e25')>=4.5,`${name} accent2 needs readable text on card background`);
}

assert.ok(v260.includes('.check.done{background:var(--accent)!important'),'completed set must follow theme accent');
assert.ok(v260.includes('.cal-cell.completed{border-color:var(--line)!important;background:#11151b!important'),'completed calendar workout must use neutral check-only styling');
assert.ok(v260.includes('.cal-cell.completed.tp-selected-day{border-color:var(--accent)!important'),'a selected completed day must still use the selection accent');
assert.ok(v260.includes('.cal-cell.skipped.tp-selected-day{opacity:.82}'),'selected skipped day must stay distinguishable');
assert.ok(v260.includes('RF260_NAV_COMPACT_ENTER=56,RF260_NAV_COMPACT_EXIT=8'),'compact header must use hysteresis to avoid mid-scroll layout jumps');

console.log(`PASS 2.6.2 responsive/theme guard: ${selectCount} dropdowns, 16 themes, stable sticky/safe-area/modal markers.`);

