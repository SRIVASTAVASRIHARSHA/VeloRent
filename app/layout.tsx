import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "DriveEase | Find a car for every plan",
  description: "A thoughtful, simple car rental experience.",
};

export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return <html lang="en"><body>{children}</body></html>;
}
