const assert=require('node:assert/strict');
const runtime=require('./helpers/app-runtime.cjs');

const app=runtime(),calls=[];
app.context.recordRefresh=()=>calls.push({tab:app.run('state.tab'),background:app.context.window.TrainPilotUIRefresh.isBackground()});
app.run('render=function(){recordRefresh()};rf245CoachVisible=false;state.session=null;state.workout=null;state.tab="home";');
app.context.window.TrainPilotUIRefresh.refresh();
assert.deepEqual(calls,[{tab:'home',background:true}]);
assert.equal(app.context.window.TrainPilotUIRefresh.isBackground(),false);
for(const setup of ['state.session={started:"active"}','state.session=null;state.tab="profile"','state.tab="plan";state.workout="day"','state.workout=null;window.TrainPilotBackupBusy=true','window.TrainPilotBackupBusy=false;document.hidden=true']){
 app.run(setup);app.context.window.TrainPilotUIRefresh.refresh();assert.equal(calls.length,1,'unfinished work must survive '+setup);
}
app.run('document.hidden=false;state.tab="health";');app.context.window.TrainPilotUIRefresh.refresh();assert.equal(calls.length,2);assert.equal(calls[1].tab,'health');
app.run('render=function(){throw Error("paint-failure")}');assert.throws(()=>app.context.window.TrainPilotUIRefresh.refresh(),/paint-failure/);assert.equal(app.context.window.TrainPilotUIRefresh.isBackground(),false,'a paint exception cannot leave user actions marked as background');

const nav=runtime(),closed=[];nav.context.window.tp155R4ClosePanel=()=>closed.push(true);
nav.run('render=function(){};state.tab="plan";state.session={started:"active"};tp2628Confirm=async()=>false;go("home");');
setImmediate(async()=>{
 assert.equal(closed.length,0,'canceling navigation must retain the open workout picker');
 nav.run('tp2628Confirm=async()=>true;go("home");');await new Promise(resolve=>setImmediate(resolve));
 assert.equal(closed.length,1);assert.equal(nav.run('state.session'),null);assert.equal(nav.run('state.tab'),'home');
 console.log('PASS background UI refresh preserves unfinished work and refreshes data pages; canceled navigation retains panels, confirmed navigation releases them');
});
