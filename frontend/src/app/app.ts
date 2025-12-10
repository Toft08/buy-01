import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { NavbarComponent } from './components/navbar/navbar.component';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, NavbarComponent],
  template: `
    <app-navbar></app-navbar>

    <main class="main-content">
      <router-outlet></router-outlet>
    </main>

    <footer class="footer">
      <div class="footer-content">
        <p>&copy; 2024 Buy-01 Marketplace. All rights reserved.</p>
      </div>
    </footer>
  `,
  styles: [
    `
      .main-content {
        min-height: calc(100vh - 140px);
      }

      .footer {
        background-color: #333;
        color: white;
        padding: 20px 0;
        text-align: center;
      }

      .footer-content {
        max-width: 1200px;
        margin: 0 auto;
        padding: 0 20px;
      }
    `,
  ],
})
export class App {}
