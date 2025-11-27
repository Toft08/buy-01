import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';
import { Cart, CartItem } from '../models/cart.model';

@Injectable({
  providedIn: 'root',
})
export class CartService {
  private cartSubject = new BehaviorSubject<Cart>({
    userId: '',
    items: [],
    total: 0,
  });

  public cart$ = this.cartSubject.asObservable();

  constructor() {
    this.loadCartFromStorage();
  }

  getCart(): Cart {
    return this.cartSubject.value;
  }

  addToCart(item: CartItem): void {
    const currentCart = this.cartSubject.value;
    const existingItem = currentCart.items.find((i) => i.productId === item.productId);

    if (existingItem) {
      existingItem.quantity += item.quantity;
    } else {
      currentCart.items.push(item);
    }

    this.updateCartTotal(currentCart);
    this.cartSubject.next(currentCart);
    this.saveCartToStorage();
  }

  removeFromCart(productId: string): void {
    const currentCart = this.cartSubject.value;
    currentCart.items = currentCart.items.filter((i) => i.productId !== productId);

    this.updateCartTotal(currentCart);
    this.cartSubject.next(currentCart);
    this.saveCartToStorage();
  }

  updateQuantity(productId: string, quantity: number): void {
    const currentCart = this.cartSubject.value;
    const item = currentCart.items.find((i) => i.productId === productId);

    if (item) {
      if (quantity <= 0) {
        this.removeFromCart(productId);
      } else {
        item.quantity = quantity;
        this.updateCartTotal(currentCart);
        this.cartSubject.next(currentCart);
        this.saveCartToStorage();
      }
    }
  }

  clearCart(): void {
    const emptyCart: Cart = {
      userId: this.cartSubject.value.userId,
      items: [],
      total: 0,
    };
    this.cartSubject.next(emptyCart);
    this.saveCartToStorage();
  }

  setUserId(userId: string): void {
    const currentCart = this.cartSubject.value;
    currentCart.userId = userId;
    this.cartSubject.next(currentCart);
  }

  getCartItemCount(): number {
    return this.cartSubject.value.items.reduce((sum, item) => sum + item.quantity, 0);
  }

  private updateCartTotal(cart: Cart): void {
    cart.total = cart.items.reduce((sum, item) => sum + item.price * item.quantity, 0);
  }

  private saveCartToStorage(): void {
    localStorage.setItem('cart', JSON.stringify(this.cartSubject.value));
  }

  private loadCartFromStorage(): void {
    const cartStr = localStorage.getItem('cart');
    if (cartStr) {
      try {
        const cart = JSON.parse(cartStr);
        this.cartSubject.next(cart);
      } catch (error) {
        this.clearCart();
      }
    }
  }
}
