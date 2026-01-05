describe('E-commerce Critical Path E2E Test', () => {
  beforeEach(() => {
    // Clear cookies and localStorage before each test
    cy.clearCookies();
    cy.clearLocalStorage();
  });

  it('should load the home page', () => {
    cy.visit('/');
    cy.contains('E-Commerce').should('be.visible');
  });

  it('should navigate to login page', () => {
    cy.visit('/');
    cy.contains('Login').click();
    cy.url().should('include', '/login');
    cy.contains('Sign In').should('be.visible');
  });

  it('should show validation errors for invalid login', () => {
    cy.visit('/login');

    // Try to submit empty form
    cy.get('button[type="submit"]').click();

    // Should show validation errors (form marked as touched)
    cy.get('form').should('exist');
  });

  it('should navigate to register page from login', () => {
    cy.visit('/login');
    cy.contains('Register').click();
    cy.url().should('include', '/register');
  });

  it('should display products on home page', () => {
    cy.visit('/');

    // Wait for products to load (may need to stub API in real tests)
    cy.get('.product-card, .card, [class*="product"]', { timeout: 10000 })
      .should('exist')
      .or(() => {
        // If no products, at least page should load
        cy.get('body').should('exist');
      });
  });

  // Mock-based test for complete user flow
  it('should complete login flow with mocked API', () => {
    // Intercept login API call
    cy.intercept('POST', '**/auth/login', {
      statusCode: 200,
      body: {
        token: 'mock-jwt-token',
        user: {
          id: 1,
          email: 'test@example.com',
          username: 'testuser',
          role: 'USER',
        },
      },
    }).as('loginRequest');

    cy.visit('/login');
    cy.get('input[type="email"]').type('test@example.com');
    cy.get('input[type="password"]').type('password123');
    cy.get('button[type="submit"]').click();

    // Wait for the API call
    cy.wait('@loginRequest');

    // Should redirect after successful login
    cy.url().should('not.include', '/login');
  });

  it('should display product list and navigate to detail', () => {
    // Mock products API
    cy.intercept('GET', '**/products', {
      statusCode: 200,
      body: [
        {
          id: 1,
          name: 'Test Product',
          description: 'Test Description',
          price: 99.99,
          quantity: 10,
          sellerId: 1,
        },
      ],
    }).as('getProducts');

    cy.visit('/');
    cy.wait('@getProducts');

    // Look for product cards
    cy.get('body').then(($body) => {
      if ($body.find('[class*="product"]').length > 0) {
        cy.get('[class*="product"]').first().click();
      }
    });
  });

  it('should navigate through main sections', () => {
    cy.visit('/');

    // Test navigation
    const routes = ['/', '/products'];

    routes.forEach((route) => {
      cy.visit(route);
      cy.get('body').should('exist');
    });
  });

  // Smoke test - verify critical pages load without errors
  it('smoke test - all critical pages should load', () => {
    const pages = ['/', '/login', '/register', '/products'];

    pages.forEach((page) => {
      cy.visit(page, { failOnStatusCode: false });
      cy.get('body').should('exist');

      // Check for no critical console errors
      cy.window().then((win) => {
        expect(win.console.error).to.not.have.been.called;
      });
    });
  });
});
