"use client";

import { useEffect, useMemo, useState } from "react";
import { ArrowRight, ArrowUpRight, CalendarDays, CarFront, Check, ChevronDown, Clock3, Fuel, Heart, MapPin, Menu, Search, ShieldCheck, SlidersHorizontal, Sparkles, Star, Users, X, Zap } from "lucide-react";
import { cars, categories, type Car } from "@/lib/cars";

const money = (n: number) => "₹" + n.toLocaleString("en-IN");

export default function Home() {
  const [activeCategory, setActiveCategory] = useState("All cars");
  const [carsData, setCarsData] = useState<Car[]>(cars);
  const [apiConnected, setApiConnected] = useState(false);
  const [customerName, setCustomerName] = useState("");
  const [historyName, setHistoryName] = useState("");
  const [customerBookings, setCustomerBookings] = useState<Array<Record<string, any>>>([]);
  const apiBase = process.env.NEXT_PUBLIC_API_BASE_URL || "http://localhost:8080/api";
  useEffect(() => {
    let cancelled = false;
    fetch(apiBase + "/vehicles").then(r => { if (!r.ok) throw new Error("API unavailable"); return r.json(); }).then((items: any[]) => {
      if (cancelled) return;
      setCarsData(items.map((item, i) => { const sample = cars.find(c => c.name.toLowerCase() === String(item.name).toLowerCase()) || cars[i % cars.length]; return { id:Number(item.id), name:String(item.name), type:String(item.type), seats:Number(item.seats), fuel:String(item.fuel), transmission:String(item.transmission || "Not specified"), price:Number(item.price), rating:Number(item.rating), trips:Number(item.totalRentals || 0), image:sample.image, tag:sample.tag, description:String(item.description || sample.description), available:Boolean(item.available) }; }));
      setApiConnected(true);
    }).catch(() => { if (!cancelled) setApiConnected(false); });
    return () => { cancelled = true; };
  }, [apiBase]);
  const [search, setSearch] = useState("");
  const [sort, setSort] = useState("recommended");
  const [selected, setSelected] = useState<Car | null>(null);
  const [booking, setBooking] = useState<Car | null>(null);
  const [liked, setLiked] = useState<number[]>([]);
  const [pickup, setPickup] = useState("Vijayawada");
  const [pickupDate, setPickupDate] = useState("2026-11-20");
  const [returnDate, setReturnDate] = useState("2026-11-22");
  const [mobileMenu, setMobileMenu] = useState(false);
  const [toast, setToast] = useState("");

  const filtered = useMemo(() => {
    let result = carsData.filter(car =>
      (activeCategory === "All cars" || car.type === activeCategory) &&
      (car.name.toLowerCase().includes(search.toLowerCase()) || car.type.toLowerCase().includes(search.toLowerCase())) && car.available !== false
    );
    if (sort === "price-low") result = [...result].sort((a,b) => a.price-b.price);
    if (sort === "price-high") result = [...result].sort((a,b) => b.price-a.price);
    if (sort === "rating") result = [...result].sort((a,b) => b.rating-a.rating);
    return result;
  }, [carsData, activeCategory, search, sort]);

  function notify(message: string) {
    setToast(message);
    window.setTimeout(() => setToast(""), 2600);
  }

  async function loadBookings() {
    if (!historyName.trim()) { notify("Enter the name used for the booking."); return; }
    try {
      const response = await fetch(apiBase + "/bookings?customer=" + encodeURIComponent(historyName.trim()));
      const data = await response.json();
      if (!response.ok) throw new Error(data.error || "Could not load bookings.");
      setCustomerBookings(data);
    } catch (error) { notify(error instanceof Error ? error.message : "Could not reach the Java API."); }
  }

  async function updateBooking(id: number, action: "cancel" | "return") {
    try {
      const response = await fetch(apiBase + "/bookings/" + id + "/" + action, {
        method: "POST", headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ customer: historyName.trim() }),
      });
      const data = await response.json();
      if (!response.ok) throw new Error(data.error || "Booking update failed.");
      notify("Booking " + action + " completed.");
      await loadBookings();
    } catch (error) { notify(error instanceof Error ? error.message : "Could not update booking."); }
  }

  async function searchAvailability() {
    const days = Math.ceil((new Date(returnDate + "T00:00:00").getTime() - new Date(pickupDate + "T00:00:00").getTime()) / 86400000);
    if (!Number.isFinite(days) || days < 1) { notify("Choose a valid return date after pickup."); return; }
    try {
      const response = await fetch(apiBase + "/vehicles?startDate=" + encodeURIComponent(pickupDate) + "&days=" + days);
      const items = await response.json();
      if (!response.ok) throw new Error(items.error || "Availability search failed.");
      setCarsData(items.map((item: any, i: number) => {
        const sample = cars.find(c => c.name.toLowerCase() === String(item.name).toLowerCase()) || cars[i % cars.length];
        return { id:Number(item.id), name:String(item.name), type:String(item.type), seats:Number(item.seats), fuel:String(item.fuel), transmission:String(item.transmission || "Not specified"), price:Number(item.price), rating:Number(item.rating), trips:Number(item.totalRentals || 0), image:sample.image, tag:sample.tag, description:String(item.description || sample.description), available:Boolean(item.available) };
      }));
      setApiConnected(true);
      document.getElementById("explore")?.scrollIntoView({behavior:"smooth"});
      notify("Availability updated for your selected dates.");
    } catch (error) { notify(error instanceof Error ? error.message : "Could not reach the Java API."); }
  }

  async function confirmBooking() {
    if (!booking) return;
    if (!customerName.trim()) { notify("Enter your name to continue."); return; }
    const days = Math.ceil((new Date(returnDate + "T00:00:00").getTime() - new Date(pickupDate + "T00:00:00").getTime()) / 86400000);
    if (!Number.isFinite(days) || days < 1) { notify("Choose a valid return date after pickup."); return; }
    try {
      const response = await fetch(apiBase + "/bookings", { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ customer: customerName.trim(), vehicleId: booking.id, startDate: pickupDate, days }) });
      const data = await response.json();
      if (!response.ok) throw new Error(data.error || "Booking could not be completed.");
      setBooking(null);
      notify("Booking confirmed! Your booking ID is " + data.id + ".");
    } catch (error) { notify(error instanceof Error ? error.message : "Could not reach the Java API. Start the backend first."); }
  }

  return (
    <main>
      <header className="topbar">
        <a className="brand" href="#"><span className="brand-mark"><CarFront size={23}/></span><span>drive<span className="brand-accent">ease</span></span></a>
        <nav className={mobileMenu ? "nav-links open" : "nav-links"}>
          <a href="#explore" onClick={() => setMobileMenu(false)}>Explore cars</a>
          <a href="#how" onClick={() => setMobileMenu(false)}>How it works</a>
          <a href="#benefits" onClick={() => setMobileMenu(false)}>Why DriveEase</a>
          <a href="#my-bookings" onClick={() => setMobileMenu(false)}>My bookings</a><a href="/admin" onClick={() => setMobileMenu(false)}>Admin</a><a href="/about" onClick={() => setMobileMenu(false)}>About project</a>
        </nav>
        <div className="nav-actions"><button className="login-btn" onClick={() => notify("Sign-in can be connected to your backend next.")}>Log in</button><button className="host-btn" onClick={() => notify("Host onboarding is coming soon.")}>List your car <ArrowUpRight size={15}/></button></div>
        <button className="mobile-toggle" aria-label="Toggle menu" onClick={() => setMobileMenu(!mobileMenu)}>{mobileMenu ? <X/> : <Menu/>}</button>
      </header>

      <section className="hero">
        <div className="hero-copy">
          <div className="eyebrow"><Sparkles size={14}/> YOUR NEXT JOURNEY STARTS HERE</div>
          <h1>Good drives.<br/><span>Great stories.</span></h1>
          <p>Find a car that fits your plans. From quick city trips to long weekend escapes, your perfect ride is just a few clicks away.</p>
          <div className="hero-points"><span><Check size={15}/> Easy booking</span><span><Check size={15}/> Flexible plans</span><span><Check size={15}/> Clear pricing</span></div>
        </div>
        <div className="hero-visual">
          <div className="hero-blob"></div>
          <img src="https://images.unsplash.com/photo-1503376780353-7e6692767b70?auto=format&fit=crop&w=1500&q=90" alt="Premium sports car on a scenic road" />
          <div className="floating-note"><span className="note-icon"><Star size={16} fill="currentColor"/></span><div><strong>Made for the journey</strong><small>Find your kind of drive</small></div></div>
          <div className="hero-stamp">LET'S<br/>GO <ArrowUpRight size={20}/></div>
        </div>
        <div className="search-panel">
          <div className="search-field location-field"><label><MapPin size={16}/> PICK-UP LOCATION</label><input value={pickup} onChange={e=>setPickup(e.target.value)} placeholder="Where are you starting?"/></div>
          <div className="search-field"><label><CalendarDays size={16}/> PICK-UP DATE</label><input type="date" value={pickupDate} onChange={e=>setPickupDate(e.target.value)}/></div>
          <div className="search-field"><label><CalendarDays size={16}/> RETURN DATE</label><input type="date" value={returnDate} min={pickupDate} onChange={e=>setReturnDate(e.target.value)}/></div>
          <button className="search-submit" onClick={searchAvailability}><Search size={18}/> Find my car</button>
        </div>
      </section>

      <section className="trust-strip" id="benefits"><div><ShieldCheck/><span><strong>{apiConnected ? "Database connected" : "Demo catalogue fallback"}</strong><small>{apiConnected ? "Live MySQL vehicle data" : "Start Java API for live data"}</small></span></div>
        <div><ShieldCheck/><span><strong>Clear & simple</strong><small>No confusing surprises</small></span></div>
        <div><Clock3/><span><strong>More flexibility</strong><small>Plans that fit your day</small></span></div>
        <div><Zap/><span><strong>Quick discovery</strong><small>Find your ride faster</small></span></div>
        <div><Heart/><span><strong>Made for you</strong><small>Choices for every journey</small></span></div>
      </section>

      <section className="explore-section" id="explore">
        <div className="section-heading">
          <div><div className="eyebrow">THE RIGHT RIDE IS WAITING</div><h2>Find your <span>perfect drive.</span></h2><p>Thoughtfully picked cars for every kind of plan.</p></div>
          <a className="text-link" href="#all-cars">View all cars <ArrowRight size={17}/></a>
        </div>
        <div className="browse-toolbar">
          <div className="category-tabs">{categories.map(c=><button key={c} className={activeCategory===c?"category active":"category"} onClick={()=>setActiveCategory(c)}>{c}</button>)}</div>
          <div className="browse-controls"><div className="mini-search"><Search size={16}/><input value={search} onChange={e=>setSearch(e.target.value)} placeholder="Search cars"/></div><div className="sort-select"><SlidersHorizontal size={15}/><select value={sort} onChange={e=>setSort(e.target.value)}><option value="recommended">Recommended</option><option value="price-low">Price: low to high</option><option value="price-high">Price: high to low</option><option value="rating">Top rated</option></select><ChevronDown size={14}/></div></div>
        </div>
        <div className="car-grid" id="all-cars">
          {filtered.map(car=><article className="car-card" key={car.id}>
            <div className="car-image-wrap"><img src={car.image} alt={car.name}/>{car.tag && <span className="car-tag">{car.tag}</span>}<button className={liked.includes(car.id)?"like-btn liked":"like-btn"} aria-label="Save car" onClick={()=>setLiked(prev=>prev.includes(car.id)?prev.filter(x=>x!==car.id):[...prev,car.id])}><Heart size={17} fill={liked.includes(car.id)?"currentColor":"none"}/></button></div>
            <div className="car-info"><div className="car-title-row"><div><h3>{car.name}</h3><span className="car-type">{car.type}</span></div><div className="rating"><Star size={14} fill="currentColor"/>{car.rating}</div></div>
              <div className="car-specs"><span><Users size={15}/>{car.seats} seats</span><span><Fuel size={15}/>{car.fuel}</span><span><CarFront size={15}/>{car.transmission}</span></div>
              <div className="car-card-bottom"><div><strong>{money(car.price)}</strong><small> / day</small></div><button onClick={()=>setSelected(car)}>View details <ArrowUpRight size={15}/></button></div>
            </div>
          </article>)}
          {filtered.length===0 && <div className="empty-state"><CarFront size={34}/><h3>No cars found</h3><p>Try another search or category.</p><button onClick={()=>{setSearch("");setActiveCategory("All cars")}}>Clear filters</button></div>}
        </div>
      </section>

      <section className="explore-section" id="my-bookings">
        <div className="section-heading"><div><div className="eyebrow">YOUR DRIVE, YOUR HISTORY</div><h2>Manage your <span>bookings.</span></h2><p>Use the same name you entered when making your reservation.</p></div></div>
        <div className="browse-controls" style={{marginTop:24}}>
          <div className="mini-search"><input value={historyName} onChange={e=>setHistoryName(e.target.value)} placeholder="Customer name"/></div>
          <button className="search-submit" onClick={loadBookings}>Find my bookings</button>
        </div>
        {customerBookings.length > 0 && <div style={{display:"grid",gap:12,marginTop:20}}>
          {customerBookings.map(b=><article key={b.id} className="car-card" style={{padding:18,display:"flex",justifyContent:"space-between",gap:16,alignItems:"center",flexWrap:"wrap"}}>
            <div><strong>#{b.id} · {b.vehicleName}</strong><p style={{margin:"7px 0",color:"#89959a"}}>{b.startDate} to {b.returnDate} · {b.days} day(s) · {b.status}</p><small>Total incl. deposit: {money(Number(b.total))}</small></div>
            {b.status === "ACTIVE" && <div style={{display:"flex",gap:8}}><button className="category" onClick={()=>updateBooking(Number(b.id),"cancel")}>Cancel</button><button className="category active" onClick={()=>updateBooking(Number(b.id),"return")}>Return car</button></div>}
          </article>)}
        </div>}
        {customerBookings.length === 0 && historyName && <p style={{color:"#89959a",marginTop:18}}>No bookings loaded yet. Enter your name and select Find my bookings.</p>}
      </section>

      <section className="how-section" id="how">
        <div className="how-intro"><div className="eyebrow">A BETTER WAY TO GET AROUND</div><h2>Three steps.<br/><span>One happy journey.</span></h2><p>We keep the process simple, so you can spend less time planning and more time enjoying the drive.</p><a className="text-link" href="#explore">Explore available cars <ArrowRight size={17}/></a></div>
        <div className="steps">
          <div className="step-card"><span className="step-no">01</span><span className="step-icon mint"><Search/></span><h3>Find your fit</h3><p>Choose your dates and explore cars that match your plans and budget.</p></div>
          <div className="step-card"><span className="step-no">02</span><span className="step-icon peach"><CalendarDays/></span><h3>Make it yours</h3><p>Review the details, understand the price and confirm your booking.</p></div>
          <div className="step-card"><span className="step-no">03</span><span className="step-icon blue"><CarFront/></span><h3>Enjoy the ride</h3><p>Pick up your car and make the most of wherever the road takes you.</p></div>
        </div>
      </section>

      <section className="cta-band"><div><span className="eyebrow">YOUR PLANS, YOUR PACE</span><h2>Ready when you are.</h2><p>Make room for a little more adventure.</p></div><a href="#explore" className="cta-button">Find your car <ArrowRight size={17}/></a><div className="cta-decoration">✳</div></section>

      <footer className="footer"><a className="brand" href="#"><span className="brand-mark"><CarFront size={22}/></span><span>drive<span className="brand-accent">ease</span></span></a><p>Thoughtful rides for wherever life takes you.</p><div className="footer-links"><a href="#explore">Explore</a><a href="#how">How it works</a><a href="#benefits">Why DriveEase</a><a href="/about">About project</a></div><small>© 2026 DriveEase. A Smart Car Rental System project.</small></footer>

      {selected && <div className="modal-backdrop" onClick={()=>setSelected(null)}><div className="detail-modal" onClick={e=>e.stopPropagation()}><button className="modal-close" onClick={()=>setSelected(null)}><X/></button><img className="detail-image" src={selected.image} alt={selected.name}/><div className="detail-content"><div className="detail-heading"><div><span className="car-type">{selected.type}</span><h2>{selected.name}</h2></div><div className="rating"><Star size={15} fill="currentColor"/>{selected.rating}</div></div><p>{selected.description}</p><div className="detail-specs"><span><Users/>{selected.seats} seats</span><span><Fuel/>{selected.fuel}</span><span><CarFront/>{selected.transmission}</span></div><div className="detail-price"><div><strong>{money(selected.price)}</strong><small> / day</small></div><button className="search-submit" onClick={()=>{setBooking(selected);setSelected(null)}}>Continue to booking <ArrowRight size={17}/></button></div></div></div></div>}
      {booking && <div className="modal-backdrop" onClick={()=>setBooking(null)}><div className="booking-modal" onClick={e=>e.stopPropagation()}><button className="modal-close" onClick={()=>setBooking(null)}><X/></button><div className="eyebrow">ALMOST THERE</div><h2>Review your trip</h2><div className="booking-car"><img src={booking.image} alt={booking.name}/><div><strong>{booking.name}</strong><small>{booking.type} · {booking.seats} seats</small></div></div><div className="booking-line"><span>Pick-up</span><strong>{pickup} · {pickupDate}</strong></div><div className="booking-line"><span>Return</span><strong>{returnDate}</strong></div><div className="booking-line total"><span>Daily rate</span><strong>{money(booking.price)} / day</strong></div><label className="booking-line"><span>Your name</span><input value={customerName} onChange={e=>setCustomerName(e.target.value)} placeholder="Enter your full name" /></label><p className="booking-note">Your booking is saved to the local MySQL database through the Java API.</p><button className="search-submit full" onClick={confirmBooking}>Request booking <ArrowRight size={17}/></button></div></div>}
      {toast && <div className="toast"><Check size={17}/>{toast}</div>}
    </main>
  );
}
