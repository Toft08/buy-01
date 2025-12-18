import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable, of } from 'rxjs';
import { catchError, tap } from 'rxjs/operators';
import { environment } from '../../environments/environments';
import { AuthResponse, LoginRequest, RegisterRequest, User } from '../models/ecommerce.model';

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private apiUrl = `${environment.apiUrl}/auth`;
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
    // Clear localStorage immediately - logout should always work client-side
    this.clearCurrentUser();

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
        catchError(() => {
          // Backend call failed, but we've already cleared client state
          return of('Logged out locally');
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
        // Temporarily set user for immediate UI rendering
        this.currentUserSubject.next(user);
        // Validate token with backend asynchronously
        this.validateToken(user);
      } catch (error) {
        this.clearCurrentUser();
      }
    }
  }

  /**
   * Validates the stored token with the backend.
   * If token is expired or blacklisted, clears the session.
   */
  private validateToken(user: User): void {
    const token = localStorage.getItem('token');
    if (!token || !user.id) {
      return;
    }

    // Call backend to verify token is still valid
    // Using the user endpoint as a validation check
    this.http
      .get<User>(`${environment.apiUrl}/users/${user.id}`, {
        headers: new HttpHeaders({
          Authorization: `Bearer ${token}`,
        }),
      })
      .pipe(
        catchError(() => {
          // Token is invalid/expired/blacklisted - clear session
          this.clearCurrentUser();
          return of(null);
        })
      )
      .subscribe((validatedUser) => {
        if (validatedUser) {
          // Update user data in case it changed on backend
          this.setCurrentUser(validatedUser);
        }
      });
  }
}
