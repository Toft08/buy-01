import { ComponentFixture, TestBed } from '@angular/core/testing';
import { RouterModule } from '@angular/router';
import { of } from 'rxjs';
import { Product, User } from '../../models/ecommerce.model';
import { AuthService } from '../../services/auth.service';
import { MediaService } from '../../services/media.service';
import { ProductService } from '../../services/product.service';
import { HomeComponent } from './home.component';

describe('HomeComponent', () => {
  let component: HomeComponent;
  let fixture: ComponentFixture<HomeComponent>;
  let authService: jasmine.SpyObj<AuthService>;
  let productService: jasmine.SpyObj<ProductService>;
  let mediaService: jasmine.SpyObj<MediaService>;

  const mockUser: User = {
    id: '1',
    name: 'testuser',
    email: 'test@example.com',
    role: 'client',
  };

  const mockProducts: Product[] = [
    {
      id: '1',
      name: 'Test Product 1',
      description: 'Description 1',
      price: 99.99,
      quality: 10,
      user: 'seller@example.com',
    },
    {
      id: '2',
      name: 'Test Product 2',
      description: 'Description 2',
      price: 149.99,
      quality: 5,
      user: 'seller@example.com',
    },
  ];

  beforeEach(async () => {
    const authServiceSpy = jasmine.createSpyObj('AuthService', [
      'getCurrentUser',
      'isLoggedIn',
      'isSeller',
    ]);
    const productServiceSpy = jasmine.createSpyObj('ProductService', ['getAllProducts']);
    const mediaServiceSpy = jasmine.createSpyObj('MediaService', ['getMediaByProduct']);

    await TestBed.configureTestingModule({
      imports: [HomeComponent, RouterModule.forRoot([])],
      providers: [
        { provide: AuthService, useValue: authServiceSpy },
        { provide: ProductService, useValue: productServiceSpy },
        { provide: MediaService, useValue: mediaServiceSpy },
      ],
    }).compileComponents();

    authService = TestBed.inject(AuthService) as jasmine.SpyObj<AuthService>;
    productService = TestBed.inject(ProductService) as jasmine.SpyObj<ProductService>;
    mediaService = TestBed.inject(MediaService) as jasmine.SpyObj<MediaService>;

    // Default mock returns
    authService.getCurrentUser.and.returnValue(mockUser);
    authService.isLoggedIn.and.returnValue(true);
    authService.isSeller.and.returnValue(false);
    productService.getAllProducts.and.returnValue(of(mockProducts));
    mediaService.getMediaByProduct.and.returnValue(of([]));
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(HomeComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load current user on init', () => {
    fixture.detectChanges();
    expect(authService.getCurrentUser).toHaveBeenCalled();
    expect(component.currentUser).toEqual(mockUser);
  });

  it('should load featured products on init', () => {
    fixture.detectChanges();
    expect(productService.getAllProducts).toHaveBeenCalled();
    expect(component.featuredProducts.length).toBeGreaterThan(0);
  });

  it('should check if user is logged in', () => {
    const result = component.isLoggedIn();
    expect(authService.isLoggedIn).toHaveBeenCalled();
    expect(result).toBe(true);
  });

  it('should check if user is a seller', () => {
    const result = component.isSeller();
    expect(authService.isSeller).toHaveBeenCalled();
    expect(result).toBe(false);
  });

  it('should set isLoading to false after loading products', (done) => {
    fixture.detectChanges();

    setTimeout(() => {
      expect(component.isLoading).toBe(false);
      done();
    }, 100);
  });

  it('should handle empty product list', () => {
    productService.getAllProducts.and.returnValue(of([]));
    fixture.detectChanges();

    expect(component.featuredProducts).toEqual([]);
  });
});
