from pathlib import Path
import json
import re

ROOT = Path(__file__).resolve().parents[1]

# --- app.js: exercise rows become inline accordions instead of navigating away ---
p = ROOT / 'www/app.js'
s = p.read_text(encoding='utf-8')
anchor = "const TP145_VERSION='1.4.5';\nconst tp145BackupBase=makeBackup;\nmakeBackup=function(){return {...tp145BackupBase(),appVersion:TP145_VERSION}};\n\n// @section ready.js"
if anchor not in s:
    raise SystemExit('2630 app insertion anchor missing')

section = r'''const TP145_VERSION='1.4.5';
const tp145BackupBase=makeBackup;
makeBackup=function(){return {...tp145BackupBase(),appVersion:TP145_VERSION}};

// @section trainpilot-146-inline-compact.js
// TrainPilot 1.4.6 / test 2630: exercise details stay on the Edzés page and
// collapsed Journal entries use the same compact card rhythm.
if(typeof rf201ExerciseRow==='function'){
 rf201ExerciseRow=function(dayId,id,i){
  const e=byId(id),rawPlay=typeof rf260VideoButton==='function'?rf260VideoButton(id):'';
  const play=rawPlay.replace('event.stopPropagation();','event.preventDefault();event.stopPropagation();');
  const target=e?.target?`<span class="badge">${esc(e.target)}</span>`:'';
  const equipment=e?.equipment?`<span class="badge">${esc(e.equipment)}</span>`:'';
  const note=e?.notes?`<p class="small muted tp146-exercise-note">${esc(e.notes)}</p>`:'<p class="small muted tp146-exercise-note">Koppints a módosításra, ha cserélni vagy szerkeszteni szeretnéd a gyakorlatot.</p>';
  return `<details class="exercise tp146-exercise" data-tp146-day="${esc(dayId)}" data-tp146-exercise="${esc(id)}">
   <summary class="tp146-exercise-summary">
    <div class="num">${i+1}</div>
    <div class="tp146-exercise-copy"><div class="ex-name">${esc(e?.hu||id)}</div><div class="en">${esc(e?.en||'')}</div><div class="meta">${e?.sets||''}${e?.sets?' × ':''}${esc(e?.reps||'')} ${e?.weight?`• ${e.weight} ${esc(e.unit||'kg')}`:''}</div></div>
    <div class="tp-exercise-actions">${play}<span class="tp146-exercise-chevron" aria-hidden="true">⌄</span></div>
   </summary>
   <div class="tp146-exercise-body">
    ${(target||equipment)?`<div class="tp146-exercise-tags">${target}${equipment}</div>`:''}
    ${note}
    <button type="button" class="btn secondary block tp146-edit-exercise" onclick="event.stopPropagation();rf154EditSlot('${esc(dayId)}',${i})">Gyakorlat módosítása</button>
   </div>
  </details>`;
 };
}

window.TrainPilotInlineCompact={version:'2630'};
const TP146_UI_VERSION='1.4.6';
const tp146UiBackupBase=makeBackup;
makeBackup=function(){return {...tp146UiBackupBase(),appVersion:TP146_UI_VERSION}};
// @endsection trainpilot-146-inline-compact.js

// @section ready.js'''
s = s.replace(anchor, section, 1)
s = s.replace('gyakorlat • koppints a szerkesztéshez', 'gyakorlat • koppints a részletekhez', 1)
p.write_text(s, encoding='utf-8')

# --- CSS: common compact rhythm for exercise accordions and collapsed Journal ---
p = ROOT / 'www/styles.css'
css = p.read_text(encoding='utf-8')
marker = '/* TrainPilot 1.4.6 / 2630 compact accordions */'
if marker not in css:
    css += r'''

/* TrainPilot 1.4.6 / 2630 compact accordions */
details.tp146-exercise{display:block;padding:0;overflow:hidden;border-radius:15px;margin:7px 0}
details.tp146-exercise>summary{list-style:none;display:grid;grid-template-columns:44px minmax(0,1fr) auto;gap:10px;align-items:center;padding:9px 10px;cursor:pointer;user-select:none}
details.tp146-exercise>summary::-webkit-details-marker{display:none}
details.tp146-exercise .num{width:44px;height:44px;border-radius:12px}
.tp146-exercise-copy{min-width:0}
.tp146-exercise-copy .ex-name{line-height:1.16}
.tp146-exercise-copy .en{margin-top:2px}
.tp146-exercise-copy .meta{margin-top:3px}
.tp146-exercise-chevron{display:grid;place-items:center;width:28px;height:28px;border-radius:999px;border:1px solid color-mix(in srgb,var(--accent) 72%,var(--line));color:var(--accent2);font-size:19px;font-weight:950;line-height:1;transition:transform .15s ease,background .15s ease}
details.tp146-exercise[open] .tp146-exercise-chevron{transform:rotate(180deg);background:color-mix(in srgb,var(--accent) 12%,transparent)}
.tp146-exercise-body{padding:9px 10px 11px 64px;border-top:1px solid var(--line);background:color-mix(in srgb,var(--card2) 32%,var(--card))}
.tp146-exercise-tags{display:flex;flex-wrap:wrap;gap:4px;margin-bottom:4px}
.tp146-exercise-note{line-height:1.4;margin:6px 0 10px}
.tp146-edit-exercise{min-height:38px!important;padding:8px 10px!important}

/* Napló: closed rows are intentionally thin; secondary stats return when opened. */
details.rf263-history{margin:6px 0!important;border-radius:15px!important}
details.rf263-history>summary{padding:6px 8px!important}
details.rf263-history>summary .history-head{padding:9px 10px!important;border-radius:13px!important}
details.rf263-history>summary .history-title{font-size:15px!important;line-height:1.15}
details.rf263-history>summary .history-date{font-size:11px!important;margin-top:2px!important}
details.rf263-history>summary .history-title-row{gap:8px!important;align-items:center!important}
details.rf263-history:not([open]) .history-badges{display:none!important}
details.rf263-history[open] .history-badges{margin-top:6px!important;gap:4px!important}
details.rf263-history .history-badge{padding:3px 6px!important;font-size:10px!important}
details.rf263-history>summary .badge{padding:4px 7px!important;font-size:10px!important;margin:0!important}
details.rf263-history .rf103-history-side{gap:4px!important;flex-direction:row!important;align-items:center!important}
details.rf263-history .rf103-history-chevron{width:30px!important;height:28px!important;font-size:24px!important}
details.rf263-history>.history-body{padding:0 10px 12px!important}

@media(max-width:420px){
 details.tp146-exercise>summary{grid-template-columns:42px minmax(0,1fr) auto;padding:8px 9px;gap:9px}
 details.tp146-exercise .num{width:42px;height:42px}
 .tp146-exercise-body{padding-left:60px}
 details.rf263-history>summary{padding:5px 6px!important}
 details.rf263-history>summary .history-head{padding:8px 9px!important}
}
'''
p.write_text(css, encoding='utf-8')

# --- release metadata for test APK ---
p = ROOT / 'package.json'
pkg = json.loads(p.read_text(encoding='utf-8'))
pkg['version'] = '1.4.6'
p.write_text(json.dumps(pkg, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')

p = ROOT / 'SOURCE_VERSION.json'
src = json.loads(p.read_text(encoding='utf-8'))
src['version'] = '1.4.6'
src['versionCode'] = 2630
p.write_text(json.dumps(src, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')

p = ROOT / 'android/app/build.gradle'
g = p.read_text(encoding='utf-8')
g, n1 = re.subn(r'versionCode\s+\d+', 'versionCode 2630', g, count=1)
g, n2 = re.subn(r'versionName\s+"[^"]+"', 'versionName "1.4.6"', g, count=1)
if n1 != 1 or n2 != 1:
    raise SystemExit('2630 Android version markers missing')
p.write_text(g, encoding='utf-8')

p = ROOT / 'www/sw.js'
sw = p.read_text(encoding='utf-8')
if "trainpilot-v145" not in sw and "trainpilot-v146" not in sw:
    raise SystemExit('2630 service worker cache marker missing')
sw = sw.replace('trainpilot-v145', 'trainpilot-v146', 1)
p.write_text(sw, encoding='utf-8')

print('Prepared TrainPilot 1.4.6 / 2630 inline exercise + compact journal test source')
