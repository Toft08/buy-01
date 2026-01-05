import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { environment } from '../../environments/environments';
import { AuthResponse, LoginRequest, RegisterRequest, User } from '../models/ecommerce.model';
import { AuthService } from './auth.service';

describe('AuthService', () => {
  let service: AuthService;
  let httpMock: HttpTestingController;
  let router: jasmine.SpyObj<Router>;

  const mockUser: User = {
    id: '1',
    name: 'testuser',
    email: 'test@example.com',
    role: 'client',
  };

  const mockAuthResponse: AuthResponse = {
    token: 'mock-jwt-token',
    user: mockUser,
    message: 'Success',
  };

  beforeEach(() => {
    const routerSpy = jasmine.createSpyObj('Router', ['navigate']);

    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [AuthService, { provide: Router, useValue: routerSpy }],
    });

    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router) as jasmine.SpyObj<Router>;

    // Clear localStorage before each test
    localStorage.clear();
  });

  afterEach(() => {
    httpMock.verify();
    localStorage.clear();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('login', () => {
    it('should login user and store token', () => {
      const credentials: LoginRequest = {
        email: 'test@example.com',
        password: 'password123',
      };

      service.login(credentials).subscribe((response) => {
        expect(response).toEqual(mockAuthResponse);
        expect(localStorage.getItem('token')).toBe('mock-jwt-token');
      });

      const req = httpMock.expectOne(`${environment.apiUrl}/auth/login`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(credentials);
      expect(req.request.withCredentials).toBe(true);

      req.flush(mockAuthResponse);
    });

    it('should update currentUser$ observable on successful login', (done) => {
      const credentials: LoginRequest = {
        email: 'test@example.com',
        password: 'password123',
      };

      service.currentUser$.subscribe((user) => {
        if (user) {
          expect(user).toEqual(mockUser);
          done();
        }
      });

      service.login(credentials).subscribe();

      const req = httpMock.expectOne(`${environment.apiUrl}/auth/login`);
      req.flush(mockAuthResponse);
    });
  });

  describe('register', () => {
    it('should register new user and store token', () => {
      const userData: RegisterRequest = {
        name: 'newuser',
        email: 'newuser@example.com',
        password: 'password123',
        role: 'client',
      };

      service.register(userData).subscribe((response) => {
        expect(response).toEqual(mockAuthResponse);
        expect(localStorage.getItem('token')).toBe('mock-jwt-token');
      });

      const req = httpMock.expectOne(`${environment.apiUrl}/auth/register`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(userData);

      req.flush(mockAuthResponse);
    });
  });

  describe('logout', () => {
    it('should clear user data from service', () => {
      localStorage.setItem('token', 'some-token');
      localStorage.setItem('currentUser', JSON.stringify(mockUser));

      // Subscribe to trigger HTTP call
      service.logout().subscribe();

      // Verify user was cleared immediately
      expect(service.getCurrentUser()).toBeNull();

      // Verify HTTP request was made
      const req = httpMock.expectOne(`${environment.apiUrl}/auth/logout`);
      expect(req.request.method).toBe('POST');

      // Flush the response to complete the observable
      req.flush('Logged out');

      // Verify navigation to home page
      expect(router.navigate).toHaveBeenCalledWith(['/']);
    });

    it('should return false when user is not logged in', () => {
      localStorage.clear();
      service['currentUserSubject'].next(null);

      expect(service.isLoggedIn()).toBe(false);
    });
  });

  describe('getCurrentUser', () => {
    it('should return current user', () => {
      service['currentUserSubject'].next(mockUser);
      expect(service.getCurrentUser()).toEqual(mockUser);
    });

    it('should return null when no user is logged in', () => {
      service['currentUserSubject'].next(null);
      expect(service.getCurrentUser()).toBeNull();
    });
  });

  describe('isSeller', () => {
    it('should return true for seller role', () => {
      const sellerUser = { ...mockUser, role: 'seller' as const };
      service['currentUserSubject'].next(sellerUser);

      expect(service.isSeller()).toBe(true);
    });

    it('should return false for non-seller role', () => {
      service['currentUserSubject'].next(mockUser);

      expect(service.isSeller()).toBe(false);
    });

    it('should return false when no user is logged in', () => {
      service['currentUserSubject'].next(null);

      expect(service.isSeller()).toBe(false);
    });
  });

  describe('getAuthHeaders', () => {
    it('should return headers with authorization token', () => {
      localStorage.setItem('token', 'test-token');

      const headers = service.getAuthHeaders();

      expect(headers.get('Authorization')).toBe('Bearer test-token');
    });

    it('should return empty authorization when no token exists', () => {
      localStorage.removeItem('token');

      const headers = service.getAuthHeaders();

      // When no token, it returns empty string (not 'Bearer ')
      expect(headers.get('Authorization')).toBe('');
    });
  });
});
