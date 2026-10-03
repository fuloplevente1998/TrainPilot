const fs=require('node:fs'),vm=require('node:vm');
module.exports=function appRuntime(){
 const values=new Map(),alerts=[];
 const node=()=>({innerHTML:'',textContent:'',style:{setProperty(){}},dataset:{},classList:{add(){},remove(){},contains(){return false},toggle(){}},children:[],querySelector:()=>null,querySelectorAll:()=>[],appendChild(){},prepend(){},insertBefore(){},insertAdjacentElement(){},insertAdjacentHTML(){},remove(){},setAttribute(){},removeAttribute(){},closest(){return null},addEventListener(){},focus(){}});
 const root=node(),document={hidden:false,documentElement:node(),body:node(),head:node(),querySelector:s=>s==='#app'?root:null,querySelectorAll:()=>[],getElementById:()=>null,addEventListener(){},createElement:node};
 const context=vm.createContext({localStorage:{getItem:k=>values.get(k)??null,setItem:(k,v)=>values.set(k,v),removeItem:k=>values.delete(k)},document,window:{Capacitor:{isNativePlatform:()=>false},addEventListener(){},scrollTo(){},open(){}},navigator:{onLine:true,languages:['hu-HU'],language:'hu-HU'},performance:{now:()=>1},requestAnimationFrame:fn=>{fn();return 1},setInterval:()=>1,clearInterval(){},setTimeout:()=>1,clearTimeout(){},Date,crypto:require('node:crypto').webcrypto,TextEncoder,Blob,URL,alert:m=>alerts.push(m),confirm:()=>true,prompt:()=>null,console});
 const run=source=>vm.runInContext(source,context);
 run(fs.readFileSync('www/app.js','utf8'));
 return {run,values,alerts,context};
};
