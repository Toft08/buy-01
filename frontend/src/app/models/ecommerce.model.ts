export interface User {
  id?: string;
  name: string;
  email: string;
  password?: string;
  role: 'client' | 'seller';
  avatar?: string;
}

export interface Product {
  id?: string;
  name: string;
  description: string;
  price: number;
  quality: number;
  user?: string; // Owner email
}

export interface Media {
  id?: string;
  imagePath: string;
  productId: string;
  fileName: string;
  contentType: string;
  fileSize: number;
}

export interface Avatar {
  id?: string;
  imagePath: string;
  userId: string;
  fileName: string;
  contentType: string;
  fileSize: number;
}

export interface AuthResponse {
  token: string;
  user: User;
  message: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  name: string;
  email: string;
  password: string;
  role: 'client' | 'seller';
  avatar?: string;
}