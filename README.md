# DriveEase — Smart Car Rental Website

A modern, responsive car-rental website built with Next.js, React, and TypeScript.

## Overview

DriveEase is the web interface for a Smart Car Rental System. It provides a clean interface for exploring vehicles, viewing car details, filtering available options, and interacting with a demo booking flow.

## Features

- Responsive landing page and navigation
- Vehicle browsing with search and filtering
- Car information and detail views
- Demo booking interaction
- Project documentation page at `/about`

## Technology Stack

- Next.js 14
- React 18
- TypeScript
- CSS
- lucide-react

## Project Structure

```
app/
  about/
    page.tsx
  globals.css
  layout.tsx
  page.tsx
lib/
  cars.ts
.env.example
package.json
README.md
```

## Run Locally

Install Node.js and npm, then run:

```bash
npm install
npm run dev
```

Open [http://localhost:3000](http://localhost:3000).

## Backend Integration Status

The website can load the vehicle catalogue from the Java REST API and submit booking requests to the local MySQL database. The Java API also exposes customer booking history, cancellation/return operations and password-protected admin routes. The admin interface is available at `/admin`.

Do not place database credentials in frontend code. Configure any future server-side secrets through environment variables.

## Project Status

Next.js frontend, Java/JDBC/MySQL backend and an initial local REST API integration are implemented. Run the frontend and Java API together for the full local workflow. Validate the local build and database integration before treating this as production-ready.


## Run the full local application

1. Install Java 21, Maven, Node.js 20+, and MySQL 8+.
2. Start MySQL and create the local schema: `mysql -u root < backend/database/schema.sql`.
3. In a terminal, set `DB_URL`, `DB_USER`, `DB_PASSWORD`, and a private `ADMIN_PASSWORD` as described in `backend/README.txt`.
4. Start the Java REST API from `backend/`: `mvn clean compile` then `mvn exec:java -Dexec.mainClass=RentalApiServer`.
5. In the repository root, set `NEXT_PUBLIC_API_BASE_URL=http://localhost:8080/api` in `.env.local`, run `npm install`, then `npm run dev`.
6. Open `http://localhost:3000`; the admin dashboard is at `http://localhost:3000/admin`.

The database remains local to each developer's computer. Never commit `.env.local` or database credentials.
