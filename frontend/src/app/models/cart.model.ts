export interface CartItem {
  id?: string;
  productId: string;
  productName: string;
  price: number;
  quantity: number;
  image?: string;
}

export interface Cart {
  userId: string;
  items: CartItem[];
  total: number;
}
