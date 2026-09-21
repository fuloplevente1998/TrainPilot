const fs=require('fs'),assert=require('node:assert/strict');
const pkg=require('../package.json'),src=require('../SOURCE_VERSION.json');
const ui=fs.readFileSync('www/v260.js','utf8'),themes=fs.readFileSync('www/v200.js','utf8'),i18n=fs.readFileSync('www/v212.js','utf8');
const gradle=fs.readFileSync('android/app/build.gradle','utf8'),sw=fs.readFileSync('www/sw.js','utf8');
assert.equal(pkg.version,'1.1.2');assert.equal(src.version,'1.1.2');assert.equal(src.versionCode,2609);
assert.match(gradle,/versionCode\s+2609/);assert.match(gradle,/versionName\s+"1\.1\.2"/);assert.match(sw,/trainpilot-v112/);
assert.ok(ui.includes('rf260SplitStickyHeader'),'fluid header split helper missing');
assert.ok(ui.includes('tp-brand-strip'),'non-sticky brand strip missing');
assert.ok(ui.includes('RF260_NAV_COMPACT_ENTER=56,RF260_NAV_COMPACT_EXIT=8'),'visual compact hysteresis missing');
assert.ok(!ui.includes('html.tp-nav-compact .rf208-brand-row{max-height:0'),'old reflowing brand collapse must be gone');
assert.ok(themes.includes('rf200-theme-select'),'theme selector must be a shared custom-select source');
assert.ok(themes.includes('data-swatch'),'theme options must expose color swatches');
assert.equal((themes.match(/accent:'#[0-9a-fA-F]{6}'/g)||[]).length,16,'2.6.2 must expose 16 accent themes');
for(const n of ['Piros','Magenta','Levendula','Égkék','Menta','Korall']){assert.ok(themes.includes(`name:'${n}'`),`missing theme ${n}`);assert.ok(i18n.includes(`'${n}'`),`missing translation entry ${n}`)}
assert.ok(ui.includes('rf260SetSelectContent'),'custom dropdown swatch renderer missing');
assert.ok(ui.includes('tp-select-swatch'),'custom dropdown swatch styling missing');
console.log('PASS 2.6.2: reflow-free sticky nav, in-app theme dropdown and 16-color palette.');

