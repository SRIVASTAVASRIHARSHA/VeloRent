import Link from "next/link";
import { ArrowLeft, ArrowUpRight, CarFront, CheckCircle2, Code2, Database, Layers3, ShieldCheck, Workflow } from "lucide-react";

export default function AboutPage() {
  return (
    <main className="about-page">
      <header className="topbar about-topbar">
        <Link className="brand" href="/"><span className="brand-mark"><CarFront size={23}/></span><span>drive<span className="brand-accent">ease</span></span></Link>
        <nav className="nav-links about-nav">
          <Link href="/">Home</Link><Link href="/#explore">Explore cars</Link><Link href="/#how">How it works</Link>
        </nav>
        <Link className="host-btn" href="/#explore">Browse cars <ArrowUpRight size={15}/></Link>
      </header>

      <section className="about-hero">
        <div className="about-hero-inner">
          <div className="eyebrow"><Code2 size={14}/> PROJECT DOCUMENTATION</div>
          <h1>About <span>DriveEase.</span></h1>
          <p>A Smart Car Rental System designed to make discovering vehicles and understanding the rental process simpler.</p>
          <div className="about-hero-actions"><Link className="about-back" href="/"><ArrowLeft size={16}/> Back to website</Link><span className="about-status"><span/> Project overview</span></div>
        </div>
        <div className="about-hero-art"><div className="about-art-circle"><CarFront size={94}/></div><span className="about-art-tag">SMART<br/>MOBILITY</span></div>
      </section>

      <div className="about-layout">
        <aside className="about-toc">
          <span className="about-toc-title">ON THIS PAGE</span>
          <a href="#overview">01&nbsp; Overview</a><a href="#objectives">02&nbsp; Objectives</a><a href="#features">03&nbsp; Features</a><a href="#technology">04&nbsp; Technology</a><a href="#architecture">05&nbsp; Architecture</a><a href="#status">06&nbsp; Project status</a>
        </aside>

        <article className="readme-content">
          <div className="readme-label"><span className="readme-dot"/> README <span className="readme-muted">/</span> PROJECT.md</div>
          <section id="overview" className="readme-section">
            <span className="readme-number">01 — OVERVIEW</span><h2>Project overview</h2>
            <p><strong>DriveEase</strong> is the web interface for a Smart Car Rental System. It presents a simple flow for browsing available vehicle options, reviewing car details and proceeding through a booking review.</p>
            <p>The project combines a Java-based rental application and a MySQL database layer with a modern, responsive website concept. The goal is to make the rental experience clear and approachable across desktop, tablet and mobile screens.</p>
            <div className="readme-callout"><CarFront size={20}/><p><strong>Project idea</strong><br/>Bring vehicle discovery, rental information and booking workflows together in one easy-to-use experience.</p></div>
          </section>

          <section id="objectives" className="readme-section">
            <span className="readme-number">02 — OBJECTIVES</span><h2>What the project aims to do</h2>
            <ul className="readme-check-list">
              <li><CheckCircle2/> Make it easy to browse and compare vehicle options.</li><li><CheckCircle2/> Present important car details such as seating, fuel type and transmission.</li><li><CheckCircle2/> Let users filter, search and sort the displayed cars.</li><li><CheckCircle2/> Provide a clear booking review before confirmation.</li><li><CheckCircle2/> Keep the interface responsive and accessible on different screen sizes.</li>
            </ul>
          </section>

          <section id="features" className="readme-section">
            <span className="readme-number">03 — FEATURES</span><h2>Core features</h2>
            <div className="readme-feature-grid">
              <div><span className="readme-feature-icon mint"><CarFront/></span><h3>Vehicle discovery</h3><p>Browse a sample fleet with vehicle images, categories, ratings and specifications.</p></div>
              <div><span className="readme-feature-icon peach"><Workflow/></span><h3>Search & filters</h3><p>Search by car name or type, filter by category and sort the available sample listings.</p></div>
              <div><span className="readme-feature-icon blue"><ShieldCheck/></span><h3>Booking review</h3><p>Review a demo booking summary and see the estimated rental cost before proceeding.</p></div>
              <div><span className="readme-feature-icon lavender"><Layers3/></span><h3>Responsive UI</h3><p>Use the website on desktop, tablet or mobile with layouts that adapt to screen size.</p></div>
            </div>
          </section>

          <section id="technology" className="readme-section">
            <span className="readme-number">04 — TECHNOLOGY</span><h2>Technology stack</h2>
            <div className="readme-tech-table">
              <div><span>Frontend</span><strong>Next.js · React · TypeScript</strong></div><div><span>Styling</span><strong>CSS · Responsive layouts</strong></div><div><span>Icons</span><strong>Lucide React</strong></div><div><span>Backend application</span><strong>Java</strong></div><div><span>Database integration</span><strong>JDBC · MySQL</strong></div>
            </div>
            <p className="readme-small-note">The website currently uses local sample vehicle data. The Java/JDBC application is a separate console application at this stage.</p>
          </section>

          <section id="architecture" className="readme-section">
            <span className="readme-number">05 — ARCHITECTURE</span><h2>How the parts fit together</h2>
            <div className="architecture-flow">
              <div><Code2/><strong>Next.js website</strong><small>Vehicle browsing and booking interface</small></div><span className="architecture-arrow">↔</span>
              <div className="architecture-pending"><Workflow/><strong>REST API</strong><small>Integration layer to be implemented</small></div><span className="architecture-arrow">↔</span>
              <div><Database/><strong>Java + MySQL</strong><small>Application logic and persistent data</small></div>
            </div>
            <p>To connect the website to live database records, the Java application needs HTTP API endpoints. The frontend can then request vehicle data and submit booking details through that API. Database credentials must remain on the backend and should never be placed in frontend code.</p>
          </section>

          <section id="status" className="readme-section readme-last">
            <span className="readme-number">06 — PROJECT STATUS</span><h2>Current implementation status</h2>
            <div className="status-list">
              <div><CheckCircle2/><span><strong>Website UI prototype</strong><small>Implemented with sample car listings and demo interactions.</small></span><em className="status-done">Available</em></div>
              <div><CheckCircle2/><span><strong>Java + JDBC + MySQL application</strong><small>Database-connected console application prepared separately.</small></span><em className="status-done">Prepared</em></div>
              <div><span className="status-pending-icon">○</span><span><strong>Website-to-backend API</strong><small>REST endpoints and live frontend integration are the next step.</small></span><em className="status-next">Next step</em></div>
              <div><span className="status-pending-icon">○</span><span><strong>Live booking persistence from website</strong><small>Requires the API integration and end-to-end testing.</small></span><em className="status-next">Pending</em></div>
            </div>
            <div className="readme-callout next-callout"><Database size={20}/><p><strong>Integration note</strong><br/>The booking confirmation on the website is currently a demo interaction. It does not yet create a booking in MySQL.</p></div>
          </section>
          <div className="readme-footer"><span>END OF README</span><Link href="/">Return to DriveEase <ArrowUpRight size={15}/></Link></div>
        </article>
      </div>
      <footer className="footer about-footer"><Link className="brand" href="/"><span className="brand-mark"><CarFront size={22}/></span><span>drive<span className="brand-accent">ease</span></span></Link><p>Thoughtful rides for wherever life takes you.</p><small>© 2026 DriveEase. A Smart Car Rental System project.</small></footer>
    </main>
  );
}
