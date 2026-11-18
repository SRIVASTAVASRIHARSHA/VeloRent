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

The website currently uses local sample vehicle data and a demo booking interaction. The separate Java, JDBC, and MySQL console application is not yet connected to this Next.js interface. A REST API layer is required to retrieve live vehicle records and persist bookings.

Do not place database credentials in frontend code. Configure any future server-side secrets through environment variables.

## Project Status

Frontend prototype and README-style About page are implemented. Backend API integration and live booking persistence remain future work.
