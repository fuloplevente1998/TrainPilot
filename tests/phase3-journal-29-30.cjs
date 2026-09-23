const assert=require('node:assert/strict'),fs=require('node:fs');
const html=fs.readFileSync('www/index.html','utf8');
const p=html.slice(html.indexOf('/* Phase 3 — #29 + #30'));
assert.ok(p.length>5000,'Phase 3 runtime block missing');
for(const marker of [
 "TP3_VERSION='phase3-29-30-37-r6'",
 'tp3-history-ex-item',
 'tp3-photo-section',
 'tp3-stat-row',
 "state?.tab==='stats'",
 'leftSeconds',
 'rightSeconds',
 'normalizeLegacySides',
 'unifiedChevron:true',
 'statsInline:true',
 'journalCompact:true',
 'tp3HistoryToggle',
 'tp3WorkoutSummaryHtml',
 'tp3OpenExerciseEditor',
 'tp3SaveExerciseEdit',
 'tp3ExerciseAddSet',
 'tp3StartAddExercise',
 'tp3WorkoutStats',
 'journalAcceptedCard:true',
 'exerciseInlineEditor:true',
 'exerciseAdd:true',
 'fastExerciseOpen:true',
 'healthChevronAligned:true',
 'tp3ExerciseDetails',
 'tp3RefreshExerciseEditor',
 'tp3-health-toggle-copy',
 'tp3-combined-history',
 'phase3HistoryTombstones',
 'tp3DeleteHistoryWorkout',
 'tp3PlannerPanelHost',
 'tp3-planner-close',
 'plannerPanel:true'
])assert.ok(p.includes(marker),'missing Phase 3 marker: '+marker);
assert.doesNotMatch(p,/class="card tp152-history-ex tp3-history-ex"\s+open/,'Journal exercise editor must start collapsed');
assert.match(p,/rf220ProgressHtml=function\(\)/,'Statistics must be overridden inline');
assert.doesNotMatch(p,/tp3-stat-row[^]*?onclick="rf220Exercise\(/,'Phase 3 stats rows must not navigate to the old separate details screen');
assert.match(p,/content:"›"!important/,'unified arrow glyph must be the accepted right chevron');
assert.match(p,/\.rf103-history-chevron::before/,'Journal chevrons must use the unified reference arrow');
assert.match(p,/\.tp166-home-coach-chevron::before/,'Home reference chevron must use the same unified style');
assert.match(p,/tp3WorkoutSummaryHtml/,'expanded Journal card must render the accepted inline workout summary');
assert.doesNotMatch(p,/tp3-summary-edit/,'accepted Journal card must not expose a top-level Edit button');
assert.match(p,/tp3-history-ex-item/,'each logged exercise must be independently expandable');
assert.match(p,/tp3-add-exercise/,'Journal must allow adding a new exercise');
assert.match(p,/tp3ExerciseAddSet/,'existing logged exercise must allow adding sets');
assert.doesNotMatch(p,/scrollIntoView/,'opening/editing Journal must not use the old forced scrollIntoView jump');
assert.match(p,/grid-template-columns:repeat\(4,minmax\(0,1fr\)\)/,'Journal metrics must use a one-row 1x4 grid');
assert.match(p,/target\.insertAdjacentHTML\('beforeend',tp3ExerciseEditorHtml/,'exercise editor must inject locally without whole-page render');
assert.match(p,/\.rf-history-health-toggle\{[^}]*justify-content:space-between!important/,'Health toggle must align its chevron to the right like photos');
assert.match(html,/kind==='history'&&typeof window\.tp3HistoryIsDeleted/,'Drive merge must filter explicitly deleted Journal records before stale snapshots can restore them');
assert.match(p,/confirm\.addEventListener\('pointerup',accept/,'Journal delete confirmation must commit on the first physical pointer tap');
assert.match(p,/profileScreen=function\(\)\{return tp3RenderPlannerPanel/,'personal planner must render into the Phase 3 panel instead of a separate route');
assert.match(p,/window\.tp3ClosePlannerPanel/,'personal planner must expose the unified close/back path');
console.log('PASS Phase 3 static: #29 accepted compact expandable Journal summary/editor + Health + persistent deletion, #30 inline stats/chevrons, #37 personal-planner panel.');