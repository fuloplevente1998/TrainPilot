/* #163 Step 5: on-demand, device-local journal route rendering.
 * No third-party map code or location request runs until the user opens a saved route.
 * On unreliable/offline networks, an offline SVG shows the same stored points. */
(function () {
  'use strict';
  var KEY=/^[A-Za-z0-9:._|+\-]{1,200}$/;
  var MAP_ID='tp163-route-overlay';
  var labels={
    hu:{open:'GPS-útvonal megtekintése',title:'Edzésútvonal',close:'Bezárás',loading:'Mentett útvonal betöltése…',none:'Ehhez az edzéshez még nincs mentett GPS-útvonal.',offline:'Offline útvonalnézet – térképháttér nélkül',partial:'Az útvonal részleges. Egyes GPS-pontok hiányozhatnak.',failed:'Az útvonal jelenleg nem olvasható.',km:'Távolság',points:'GPS-pont',signal:'Utolsó pontosság',map:'Térkép',start:'Rajt',finish:'Cél'},
    en:{open:'View GPS route',title:'Workout route',close:'Close',loading:'Loading saved route…',none:'No saved GPS route for this workout yet.',offline:'Offline route view – no map tiles',partial:'Partial route. Some GPS fixes might be missing.',failed:'The route cannot be read right now.',km:'Distance',points:'GPS fixes',signal:'Last accuracy',map:'Map',start:'Start',finish:'Finish'},
    de:{open:'GPS-Route anzeigen',title:'Trainingsroute',close:'Schließen',loading:'Gespeicherte Route laden…',none:'Keine gespeicherte GPS-Route vorhanden.',offline:'Offline-Route – ohne Kartenkacheln',partial:'Unvollständige Route.',failed:'Route derzeit nicht verfügbar.',km:'Distanz',points:'GPS-Punkte',signal:'Letzte Genauigkeit',map:'Karte',start:'Start',finish:'Ziel'},
    ro:{open:'Vezi traseul GPS',title:'Traseul antrenamentului',close:'Închide',loading:'Se încarcă traseul…',none:'Nu există un traseu GPS salvat.',offline:'Traseu offline – fără harta de fundal',partial:'Traseu parțial.',failed:'Traseul nu poate fi citit.',km:'Distanță',points:'Puncte GPS',signal:'Ultima precizie',map:'Hartă',start:'Start',finish:'Sosire'}
  };
  var t=function(k){var language=typeof rf212Lang==='function'?rf212Lang():'hu';return (labels[language]||labels.hu)[k]||labels.hu[k]||k;};
  function bridge(){return window.Capacitor?.Plugins?.DistanceTracker||window.Capacitor?.registerPlugin?.('DistanceTracker');}
  function references(workout){
    var rows=[],seen=new Set();
    for(var exercise of workout?.exercises||[])for(var set of exercise?.sets||[]){
      var key=set?.gpsRouteKey163;
      if(typeof key==='string'&&KEY.test(key)&&!seen.has(key)){
        rows.push({key:key,exercise:exercise.id||'',setId:set.setId||''});seen.add(key);
      }
    }
    return rows.slice(0,32);
  }
  function append(tag,parent,classname,txt){
    var node=document.createElement(tag);
    if(classname)node.className=classname;
    if(txt!=null)node.textContent=String(txt);
    if(parent)parent.appendChild(node);
    return node;
  }
  function mountRow(row){
    if(!row||row.querySelector('.tp163-route-entry'))return;
    var detail=row.closest('details[data-tp152-history-key]'),key=detail?.dataset.tp152HistoryKey;
    if(!key)return;
    var workout=history().find(function(v){return rf142WorkoutKey(v)===key;});
    var refs=references(workout);
    if(!refs.length)return;
    var entry=append('div',null,'tp163-route-entry');
    var button=append('button',entry,'btn secondary block tp163-route-button','↗ '+t('open'));
    button.type='button';
    button.addEventListener('click',function(event){event.preventDefault();event.stopPropagation();openRoute(key);});
    row.prepend(entry);
  }
  function mountAll(){
    if(typeof state==='undefined'||state.tab!=='history')return;
    document.querySelectorAll('details[data-tp152-history-key] > .tp3-combined-history-body[data-tp3-hydrated="true"]').forEach(mountRow);
  }
  function coordinates(result){
    var sections=[],count=0,partial=!!result?.partial,accuracy=null;
    for(var raw of result?.segments||[]){
      if(sections.length>=256||count>=20000){partial=true;break;}
      if(!Array.isArray(raw.points))continue;
      var pts=[];
      for(var p of raw.points){
        if(count>=20000){partial=true;break;}
        var lat=Number(p.latitude),lon=Number(p.longitude),acc=Number(p.accuracyMeters);
        if(!Number.isFinite(lat)||!Number.isFinite(lon)||Math.abs(lat)>85||Math.abs(lon)>180)continue;
        pts.push([lat,lon]);accuracy=Number.isFinite(acc)&&acc>=0?acc:accuracy;
        count++;
      }
      if(pts.length)sections.push(pts);
    }
    return {sections:sections,count:count,partial:partial,accuracy:accuracy};
  }
  function mercator(lat){var a=Math.min(85,Math.max(-85,lat))*Math.PI/180;return Math.log(Math.tan(Math.PI/4+a/2));}
  function drawOffline(host,sections){
    var NS='http://www.w3.org/2000/svg',svg=document.createElementNS(NS,'svg');
    svg.setAttribute('viewBox','0 0 400 460');
    svg.setAttribute('class','tp163-route-svg');
    svg.setAttribute('role','img');svg.setAttribute('aria-label',t('offline'));
    var points=sections.flat().map(function(p){return [p[1]*Math.PI/180,-mercator(p[0])];});
    if(!points.length){append('p',host,'tp163-empty',t('none'));return;}
    var xs=points.map(function(p){return p[0];}),ys=points.map(function(p){return p[1];});
    var minX=Math.min.apply(null,xs),maxX=Math.max.apply(null,xs),minY=Math.min.apply(null,ys),maxY=Math.max.apply(null,ys);
    var width=Math.max(maxX-minX,0.00001),height=Math.max(maxY-minY,0.00001);
    var scale=Math.min(352/width,410/height),cx=(minX+maxX)/2,cy=(minY+maxY)/2;
    var xy=function(p){return [200+(p[1]*Math.PI/180-cx)*scale,230+(-mercator(p[0])-cy)*scale];};
    sections.forEach(function(section){
      var path=document.createElementNS(NS,'path');
      path.setAttribute('d',section.map(function(p,i){var v=xy(p);return (i?'L':'M')+v[0].toFixed(2)+' '+v[1].toFixed(2);}).join(' '));
      path.setAttribute('class','tp163-route-line');svg.appendChild(path);
    });
    [sections[0][0],sections[sections.length-1].slice(-1)[0]].forEach(function(p,i){
      var marker=document.createElementNS(NS,'circle'),v=xy(p);
      marker.setAttribute('cx',v[0]);marker.setAttribute('cy',v[1]);marker.setAttribute('r',i?7:6);
      marker.setAttribute('class',i?'tp163-route-finish':'tp163-route-start');
      svg.appendChild(marker);
    });
    host.appendChild(svg);
  }
  var leafletPromise=null;
  function getLeaflet(){
    if(window.L?.map)return Promise.resolve(window.L);
    if(leafletPromise)return leafletPromise;
    leafletPromise=new Promise(function(resolve,reject){
      var link=document.createElement('link');link.rel='stylesheet';
      link.href='https://unpkg.com/leaflet@1.9.4/dist/leaflet.css';document.head.appendChild(link);
      var script=document.createElement('script');script.src='https://unpkg.com/leaflet@1.9.4/dist/leaflet.js';
      script.referrerPolicy='no-referrer';
      var settled=false;
      var timer=setTimeout(function(){finish(false);},4800);
      function finish(ok){
        if(settled)return;settled=true;clearTimeout(timer);
        if(ok&&window.L?.map)resolve(window.L);else reject(new Error('Leaflet unavailable'));
      }
      script.onload=function(){finish(true);};script.onerror=function(){finish(false);};
      document.head.appendChild(script);
    }).catch(function(error){leafletPromise=null;throw error;});
    return leafletPromise;
  }
  var activeMap=null,previousFocus=null,oldOverflow='';
  function closeRoute(){
    if(activeMap){activeMap.remove();activeMap=null;}
    var node=document.getElementById(MAP_ID);
    if(node)node.remove();
    document.body.style.overflow=oldOverflow;
    document.removeEventListener('keydown',escapeHandler,true);
    if(previousFocus?.isConnected)previousFocus.focus({preventScroll:true});
  }
  function escapeHandler(event){if(event.key==='Escape'){event.preventDefault();closeRoute();}}
  function textMetric(parent,label,value){
    var box=append('div',parent,'tp163-metric');append('span',box,null,label);append('strong',box,null,value);
  }
  function renderLeaflet(host,segments){
    if(!navigator.onLine||!segments.length)return;
    getLeaflet().then(function(L){
      if(!host.isConnected||document.getElementById(MAP_ID)==null)return;
      var pane=append('div',null,'tp163-leaflet');
      host.replaceChildren(pane);
      var map=L.map(pane,{attributionControl:true,scrollWheelZoom:false,preferCanvas:true,zoomControl:true});
      activeMap=map;
      L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png',{
        attribution:'&copy; OpenStreetMap contributors',maxZoom:18,
        minZoom:1,updateWhenIdle:true,keepBuffer:1
      }).addTo(map);
      var coords=[];
      segments.forEach(function(section){
        L.polyline(section,{color:'#3be68a',weight:5,opacity:.96,lineJoin:'round'}).addTo(map);
        coords.push.apply(coords,section);
      });
      if(!coords.length)return;
      L.circleMarker(coords[0],{radius:7,color:'#e7faff',fillColor:'#30dff2',fillOpacity:1,weight:2}).bindTooltip(t('start')).addTo(map);
      L.circleMarker(coords[coords.length-1],{radius:7,color:'#ffe4b5',fillColor:'#ffb93d',fillOpacity:1,weight:2}).bindTooltip(t('finish')).addTo(map);
      var bounds=L.latLngBounds(coords);
      if(bounds.isValid())map.fitBounds(bounds.pad(.14),{maxZoom:16,animate:false});
      requestAnimationFrame(function(){if(pane.isConnected)map.invalidateSize();});
    }).catch(function(){/* The rendered offline route remains available. */});
  }
  async function openRoute(workoutKey){
    var workout=history().find(function(w){return rf142WorkoutKey(w)===workoutKey;});
    var refs=references(workout);
    if(!refs.length)return;
    closeRoute();
    previousFocus=document.activeElement;
    oldOverflow=document.body.style.overflow;
    var veil=append('div',document.body,'tp163-route-veil');
    veil.id=MAP_ID;veil.setAttribute('role','presentation');
    var modal=append('section',veil,'tp163-route-dialog');
    modal.setAttribute('role','dialog');modal.setAttribute('aria-modal','true');modal.setAttribute('aria-label',t('title'));
    var head=append('header',modal,'tp163-route-head');
    append('h2',head,null,t('title'));
    var close=append('button',head,'btn secondary tp163-close',t('close'));close.type='button';close.onclick=closeRoute;
    var metrics=append('div',modal,'tp163-route-metrics');
    var hint=append('p',modal,'tp163-route-hint',t('loading'));
    var mapHost=append('div',modal,'tp163-map-area');
    mapHost.setAttribute('aria-label',t('map'));
    veil.addEventListener('click',function(event){if(event.target===veil)closeRoute();});
    document.addEventListener('keydown',escapeHandler,true);
    document.body.style.overflow='hidden';close.focus();
    var plugin=bridge();
    if(!plugin?.route){hint.textContent=t('failed');return;}
    try{
      var replies=[];
      for(var ref of refs)replies.push(await plugin.route({key:ref.key}));
      if(!mapHost.isConnected)return;
      var sections=[],pointCount=0,partial=false,accuracy=null;
      for(var reply of replies){
        var valid=coordinates(reply);
        pointCount+=valid.count;partial=partial||valid.partial;
        if(valid.accuracy!==null)accuracy=valid.accuracy;
        sections.push.apply(sections,valid.sections);
      }
      hint.textContent=partial?t('partial'):t('offline');
      var meters=(workout.exercises||[]).reduce(function(sum,e){
        return sum+(e.sets||[]).reduce(function(acc,set){
          return acc+(set.gpsRouteKey163&&Number.isFinite(Number(set.distanceMeters))?Number(set.distanceMeters):0);
        },0);
      },0);
      textMetric(metrics,t('km'),(meters/1000).toLocaleString('hu-HU',{maximumFractionDigits:2})+' km');
      textMetric(metrics,t('points'),String(pointCount));
      if(accuracy!==null)textMetric(metrics,t('signal'),'± '+Math.round(accuracy)+' m');
      drawOffline(mapHost,sections);
      if(pointCount)renderLeaflet(mapHost,sections);
    }catch(_){if(mapHost.isConnected)hint.textContent=t('failed');}
  }
  function install(){
    // Keep the per-set route owner in the saved draft/history; gpsTrip106 is temporary.
    if(typeof window.tp106ApplyGPS==='function'){
      var base=window.tp106ApplyGPS;
      window.tp106ApplyGPS=function(result){
        var trip=db.get('gpsTrip106',null),owner=typeof tp106GpsOwner==='function'?tp106GpsOwner(trip):null;
        var applied=base.apply(this,arguments);
        if(applied&&trip&&owner?.set&&result?.key===trip.key&&Number(result.routeSavedPoints)>0){
          if(owner.set.gpsRouteKey163!==trip.key){
            owner.set.gpsRouteKey163=trip.key;
            if(owner.draft){owner.draft.savedAt=new Date().toISOString();db.set('draft',owner.draft);}
            else persistDraft();
          }
        }
        return applied;
      };
    }
    if(typeof window.tp3EnsureHistoryBody==='function'){
      var ensure=window.tp3EnsureHistoryBody;
      window.tp3EnsureHistoryBody=function(){
        var body=ensure.apply(this,arguments);mountRow(body);return body;
      };
    }
    if(typeof window.render==='function'){
      var baseRender=window.render;
      window.render=function(){
        var out=baseRender.apply(this,arguments);
        if(typeof state!=='undefined'&&state.tab==='history')requestAnimationFrame(mountAll);
        return out;
      };
    }
    mountAll();
  }
  window.TrainPilotGpsMap163={references:references,coordinates:coordinates,drawOffline:drawOffline,openRoute:openRoute,closeRoute:closeRoute,mountAll:mountAll};
  if(document.readyState==='loading')window.addEventListener('DOMContentLoaded',install);
  else install();
})();
