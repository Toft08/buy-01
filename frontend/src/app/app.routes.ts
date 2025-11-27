import { Routes } from '@angular/router';
import { LoginComponent } from './components/auth/login.component';
import { RegisterComponent } from './components/auth/register.component';
import { HomeComponent } from './components/home/home.component';
import { ProductListComponent } from './components/products/product-list.component';
import { UserProfileComponent } from './components/profile/user-profile.component';
import { SellerDashboardComponent } from './components/seller/seller-dashboard.component';

export const routes: Routes = [
  { path: '', component: HomeComponent },
  { path: 'login', component: LoginComponent },
  { path: 'register', component: RegisterComponent },
  { path: 'products', component: ProductListComponent },
  { path: 'profile', component: UserProfileComponent },
  { path: 'seller/dashboard', component: SellerDashboardComponent },
  { path: '**', redirectTo: '' },
];
