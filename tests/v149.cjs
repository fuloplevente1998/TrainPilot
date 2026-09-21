const fs=require('node:fs'),assert=require('node:assert/strict');
const s=fs.readFileSync('www/trainpilot-149-complete-i18n.js','utf8');

for(const x of [
 "TP149_VERSION='1.4.9-dev'",
 "TP149_LANGS={system:'System',hu:'Magyar',en:'English',de:'Deutsch',ro:'Română'}",
 "TP149_LOCALES={hu:'hu-HU',en:'en-US',de:'de-DE',ro:'ro-RO'}",
 'TP149_KEY_ROWS','TP149_CATALOG','tp149T','window.t=tp149T','tp149Plural',
 'tp149FormatNumber','tp149FormatDate','tp149FormatDateTime','tp149FormatUnit',
 'tp149CatalogAudit','tp149UserValues','tp149TranslateDom','tp149TranslateFreshDom',
 'language:typeof rf212LangSetting',
 'Kerülendő területek','Areas to avoid','Zu vermeidende Bereiche','Zone de evitat',
 'Gyakorlatkönyvtár','Exercise library','Übungsbibliothek','Bibliotecă de exerciții'
])assert.ok(s.includes(x),'Missing '+x);

const keyBlock=s.slice(s.indexOf('const TP149_KEY_ROWS={'),s.indexOf('const TP149_CATALOG='));
const keys=[...keyBlock.matchAll(/\n\s*'([^']+)'\s*:/g)].map(m=>m[1]);
assert.ok(keys.length>=45,'expected a substantial key-based phase-1 catalog');
assert.equal(new Set(keys).size,keys.length,'duplicate i18n keys');

for(const key of ['nav.home','language.system','workout.start','planner.avoidAreas','backup.restore','exercise.library']){
 assert.ok(keyBlock.includes("'"+key+"'"),'missing key '+key);
}

assert.ok(s.includes("return value==null?String(key)"),'missing visible-key fallback');
assert.ok(s.includes("const safe=TP149_LOCALES[resolved]?resolved:'en'"),'translation fallback must remain English');
assert.ok(s.includes("TP149_EX_DE")&&s.includes("TP149_EX_RO"),'legacy exercise-name bridge missing during migration');
assert.ok(s.includes("rf212Translate=function"),'legacy translation bridge must remain until full migration');
console.log('PASS: TrainPilot 1.4.9 key-based i18n core and migration bridge.');
