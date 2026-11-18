export type Car = {
  id: number; name: string; type: string; seats: number; fuel: string;
  transmission: string; price: number; rating: number; trips: number;
  image: string; tag?: string; description: string;
};

export const cars: Car[] = [
  { id: 1, name: "Hyundai Creta", type: "SUV", seats: 5, fuel: "Petrol", transmission: "Automatic", price: 2499, rating: 4.9, trips: 128, tag: "Popular choice", image: "https://images.unsplash.com/photo-1605559424843-9e4c228bf1c2?auto=format&fit=crop&w=1200&q=85", description: "A comfortable, spacious SUV for weekend escapes and everyday adventures." },
  { id: 2, name: "Honda City", type: "Sedan", seats: 5, fuel: "Petrol", transmission: "Automatic", price: 1899, rating: 4.8, trips: 96, tag: "Great value", image: "https://images.unsplash.com/photo-1590362891991-f776e747a588?auto=format&fit=crop&w=1200&q=85", description: "A smooth and refined sedan with plenty of room for a relaxed drive." },
  { id: 3, name: "Maruti Baleno", type: "Hatchback", seats: 5, fuel: "Petrol", transmission: "Manual", price: 1199, rating: 4.7, trips: 84, image: "https://images.unsplash.com/photo-1549317661-bd32c8ce0db2?auto=format&fit=crop&w=1200&q=85", description: "Easy to drive, economical and perfect for getting around the city." },
  { id: 4, name: "Toyota Innova Crysta", type: "MUV", seats: 7, fuel: "Diesel", transmission: "Manual", price: 3299, rating: 4.9, trips: 142, tag: "Family favourite", image: "https://images.unsplash.com/photo-1519641471654-76ce0107ad1b?auto=format&fit=crop&w=1200&q=85", description: "Room for everyone, with a comfortable ride for longer journeys." },
  { id: 5, name: "Kia Seltos", type: "SUV", seats: 5, fuel: "Diesel", transmission: "Automatic", price: 2799, rating: 4.8, trips: 73, image: "https://images.unsplash.com/photo-1617469767053-d3b523a0b982?auto=format&fit=crop&w=1200&q=85", description: "A stylish SUV with a confident road presence and a premium cabin." },
  { id: 6, name: "Tata Tiago", type: "Hatchback", seats: 5, fuel: "Petrol", transmission: "Manual", price: 999, rating: 4.6, trips: 61, tag: "Budget friendly", image: "https://images.unsplash.com/photo-1503376780353-7e6692767b70?auto=format&fit=crop&w=1200&q=85", description: "A compact companion for quick errands and city plans." },
];

export const categories = ["All cars", "SUV", "Sedan", "Hatchback", "MUV"];
