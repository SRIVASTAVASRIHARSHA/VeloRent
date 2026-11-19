"use client";

import { useEffect, useState } from "react";

const API = process.env.NEXT_PUBLIC_API_BASE_URL || "http://localhost:8080/api";
type Vehicle = { id:number; name:string; type:string; fuel:string; seats:number; price:number; status:string };
type Booking = { id:number; customer:string; vehicleName:string; startDate:string; returnDate:string; status:string; total:number };
type Report = { activeBookings:number; returnedBookings:number; cancelledBookings:number; totalRevenue:number; rentalIncome:number; taxCollected:number; lateFees:number; cancellationFees:number };

export default function AdminPage() {
  const [password,setPassword] = useState("");
  const [unlocked,setUnlocked] = useState(false);
  const [vehicles,setVehicles] = useState<Vehicle[]>([]);
  const [bookings,setBookings] = useState<Booking[]>([]);
  const [report,setReport] = useState<Report|null>(null);
  const [message,setMessage] = useState("");
  const [busy,setBusy] = useState(false);

  async function request(path:string, options:RequestInit = {}) {
    const response = await fetch(API + path,{...options,headers:{ "Content-Type":"application/json","X-Admin-Password":password,...(options.headers||{}) }});
    const data = await response.json();
    if(!response.ok) throw new Error(data.error || "Request failed");
    return data;
  }
  async function load() {
    setBusy(true); setMessage("");
    try {
      const [v,b,r] = await Promise.all([fetch(API+"/vehicles").then(x=>x.json()),request("/admin/bookings"),request("/admin/reports")]);
      setVehicles(v); setBookings(b); setReport(r); setUnlocked(true);
    } catch(e) { setUnlocked(false); setMessage(e instanceof Error?e.message:"Could not load admin data"); }
    finally { setBusy(false); }
  }
  useEffect(()=>{ /* Admin data is loaded only after the password is submitted. */ },[]);
  async function addVehicle(event:React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const formElement = event.currentTarget;
    const form = new FormData(formElement);
    try {
      await request("/admin/vehicles",{method:"POST",body:JSON.stringify({category:form.get("category"),brand:form.get("brand"),model:form.get("model"),fuel:form.get("fuel"),seats:Number(form.get("seats")),pricePerDay:Number(form.get("price")),rating:Number(form.get("rating"))})});
      formElement.reset(); setMessage("Vehicle added."); await load();
    } catch(e) { setMessage(e instanceof Error?e.message:"Could not add vehicle"); }
  }
  async function changePrice(vehicle:Vehicle) {
    const value=window.prompt("New daily price (₹)",String(vehicle.price));
    if(value===null) return;
    try { await request("/admin/vehicles/"+vehicle.id+"/price",{method:"PATCH",body:JSON.stringify({pricePerDay:Number(value)})}); setMessage("Price updated."); await load(); }
    catch(e) { setMessage(e instanceof Error?e.message:"Could not update price"); }
  }
  async function removeVehicle(vehicle:Vehicle) {
    if(!window.confirm("Remove "+vehicle.name+"?")) return;
    try { await request("/admin/vehicles/"+vehicle.id,{method:"DELETE"}); setMessage("Vehicle removed."); await load(); }
    catch(e) { setMessage(e instanceof Error?e.message:"Could not remove vehicle"); }
  }
  async function maintenance(vehicle:Vehicle,complete:boolean) {
    try { await request("/admin/vehicles/"+vehicle.id+"/maintenance",{method:"POST",body:JSON.stringify({complete})}); setMessage("Maintenance status updated."); await load(); }
    catch(e) { setMessage(e instanceof Error?e.message:"Could not update maintenance"); }
  }

  return <main style={{maxWidth:1100,margin:"0 auto",padding:"40px 22px",color:"#52616a"}}>
    <a href="/" style={{color:"#78b595"}}>← Back to DriveEase</a>
    <h1 style={{fontSize:36,marginBottom:8}}>DriveEase administration</h1>
    <p style={{color:"#89959a"}}>Manage the local vehicle fleet, bookings and rental performance.</p>
    {!unlocked && <form onSubmit={e=>{e.preventDefault();load();}} style={{display:"flex",gap:10,maxWidth:480,margin:"28px 0"}}>
      <input type="password" value={password} onChange={e=>setPassword(e.target.value)} placeholder="Admin password" required style={{padding:13,border:"1px solid #dce7df",borderRadius:9,flex:1}}/>
      <button disabled={busy} style={{background:"#83c9aa",color:"white",padding:"12px 20px",borderRadius:9}}>{busy?"Checking…":"Unlock"}</button>
    </form>}
    {message && <p role="status" style={{padding:12,background:"#f0f7f2",borderRadius:9}}>{message}</p>}
    {unlocked && <>
      <section style={{display:"grid",gridTemplateColumns:"repeat(auto-fit,minmax(150px,1fr))",gap:12,margin:"28px 0"}}>
        {[["Total revenue",report?"₹"+report.totalRevenue.toLocaleString("en-IN"):"—"],["Active bookings",report?.activeBookings??0],["Completed",report?.returnedBookings??0],["Cancelled",report?.cancelledBookings??0]].map(([label,value])=><div key={String(label)} style={{padding:18,background:"#f0f7f2",borderRadius:12}}><small>{label}</small><h2 style={{margin:"8px 0"}}>{value}</h2></div>)}
      </section>
      <h2>Vehicle fleet</h2>
      <div style={{overflowX:"auto"}}><table style={{width:"100%",borderCollapse:"collapse",textAlign:"left"}}><thead><tr>{["Vehicle","Category","Fuel","Seats","Daily price","Status","Actions"].map(x=><th key={x} style={{padding:10,borderBottom:"1px solid #dce7df"}}>{x}</th>)}</tr></thead><tbody>{vehicles.map(v=><tr key={v.id}><td style={{padding:10,borderBottom:"1px solid #edf1ee"}}>{v.name}</td><td>{v.type}</td><td>{v.fuel}</td><td>{v.seats}</td><td>₹{v.price}</td><td>{v.status}</td><td><button onClick={()=>changePrice(v)}>Price</button> <button onClick={()=>maintenance(v,v.status!=="MAINTENANCE")}>{v.status==="MAINTENANCE"?"Complete service":"Maintenance"}</button> <button onClick={()=>removeVehicle(v)}>Remove</button></td></tr>)}</tbody></table></div>
      <h2 style={{marginTop:32}}>Add a vehicle</h2>
      <form onSubmit={addVehicle} style={{display:"grid",gridTemplateColumns:"repeat(auto-fit,minmax(145px,1fr))",gap:10}}>
        <select name="category" required style={{padding:11}}><option>Hatchback</option><option>Sedan</option><option>SUV</option><option>Luxury</option></select>
        <input name="brand" placeholder="Brand" required style={{padding:11}}/><input name="model" placeholder="Model" required style={{padding:11}}/>
        <select name="fuel" style={{padding:11}}><option>Petrol</option><option>Diesel</option><option>Electric</option><option>CNG</option></select>
        <input name="seats" type="number" min="2" max="12" placeholder="Seats" required style={{padding:11}}/>
        <input name="price" type="number" min="100" placeholder="Daily price" required style={{padding:11}}/>
        <input name="rating" type="number" min="0" max="5" step="0.1" placeholder="Rating" defaultValue="0" style={{padding:11}}/>
        <button style={{background:"#83c9aa",color:"white",borderRadius:8,padding:12}}>Add vehicle</button>
      </form>
      <h2 style={{marginTop:34}}>Bookings</h2>
      <div style={{overflowX:"auto"}}><table style={{width:"100%",borderCollapse:"collapse",textAlign:"left"}}><thead><tr>{["ID","Customer","Vehicle","Pickup","Due","Status","Total"].map(x=><th key={x} style={{padding:10,borderBottom:"1px solid #dce7df"}}>{x}</th>)}</tr></thead><tbody>{bookings.map(b=><tr key={b.id}><td style={{padding:10,borderBottom:"1px solid #edf1ee"}}>{b.id}</td><td>{b.customer}</td><td>{b.vehicleName}</td><td>{b.startDate}</td><td>{b.returnDate}</td><td>{b.status}</td><td>₹{b.total.toLocaleString("en-IN")}</td></tr>)}</tbody></table></div>
      <button onClick={load} style={{marginTop:22,padding:12,borderRadius:8,background:"#e5f4ec",color:"#5e9d81"}}>Refresh dashboard</button>
    </>}
  </main>;
}
