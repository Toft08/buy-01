import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';
import { tap } from 'rxjs/operators';
import { AuthResponse, LoginRequest, RegisterRequest, User } from '../models/ecommerce.model';

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private apiUrl = 'http://localhost:8080/auth';
  private currentUserSubject = new BehaviorSubject<User | null>(null);
  public currentUser$ = this.currentUserSubject.asObservable();

  constructor(private http: HttpClient) {
    this.loadUserFromStorage();
  }

  login(credentials: LoginRequest): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>(`${this.apiUrl}/login`, credentials, {
        withCredentials: true, // Include cookies
      })
      .pipe(
        tap((response) => {
          this.setCurrentUser(response.user);
          localStorage.setItem('token', response.token);
        })
      );
  }

  register(userData: RegisterRequest): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>(`${this.apiUrl}/register`, userData, {
        withCredentials: true, // Include cookies
      })
      .pipe(
        tap((response) => {
          this.setCurrentUser(response.user);
          localStorage.setItem('token', response.token);
        })
      );
  }

  logout(): Observable<string> {
    return this.http
      .post<string>(
        `${this.apiUrl}/logout`,
        {},
        {
          withCredentials: true,
          responseType: 'text' as 'json',
        }
      )
      .pipe(
        tap(() => {
          this.clearCurrentUser();
        })
      );
  }

  getCurrentUser(): User | null {
    return this.currentUserSubject.value;
  }

  isLoggedIn(): boolean {
    return this.currentUserSubject.value !== null;
  }

  isSeller(): boolean {
    const user = this.getCurrentUser();
    return user?.role === 'seller';
  }

  isClient(): boolean {
    const user = this.getCurrentUser();
    return user?.role === 'client';
  }

  getAuthHeaders(): HttpHeaders {
    const token = localStorage.getItem('token');
    return new HttpHeaders({
      Authorization: token ? `Bearer ${token}` : '',
      'Content-Type': 'application/json',
    });
  }

  /**
   * Update the current user's data (e.g., after avatar upload)
   */
  updateCurrentUser(updates: Partial<User>): void {
    const currentUser = this.currentUserSubject.value;
    if (currentUser) {
      const updatedUser = { ...currentUser, ...updates };
      this.currentUserSubject.next(updatedUser);
      localStorage.setItem('currentUser', JSON.stringify(updatedUser));
    }
  }

  private setCurrentUser(user: User): void {
    this.currentUserSubject.next(user);
    localStorage.setItem('currentUser', JSON.stringify(user));
  }

  private clearCurrentUser(): void {
    this.currentUserSubject.next(null);
    localStorage.removeItem('currentUser');
    localStorage.removeItem('token');
  }

  private loadUserFromStorage(): void {
    const userStr = localStorage.getItem('currentUser');
    const token = localStorage.getItem('token');

    if (userStr && token) {
      try {
        const user = JSON.parse(userStr);
        this.currentUserSubject.next(user);
      } catch (error) {
        this.clearCurrentUser();
      }
    }
  }
}
