import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../../services/auth/auth';
import {NgIf} from '@angular/common';
import {Router} from '@angular/router';

@Component({
  standalone: true,
  selector: 'app-login',
  imports: [
    FormsModule,
    NgIf
  ],
  templateUrl: './login.html',
  styleUrl: './login.css'
})

export class Login {

  loginModel = {
    email: '',
    password: ''
  };

  errorMsg: string | null = null;
  constructor(private authService: AuthService, private router: Router) { }


  onLoginSubmit() {
    this.authService.login(this.loginModel)
      .subscribe({
          next: (response: any) => {

            if (response?.status === 'REQUEST_STATUS_CHECK') {
              this.authService.setPendingRegistrationEmail(this.loginModel.email);
              this.router.navigate(['/request-pending']);
              this.errorMsg = null;
              return;
            }

            console.log('Login successful, token saved');
            this.errorMsg = null;
            this.authService.saveTokenAndRedirect(response.jwt);
          },

          error: (err: any) => {
            const errorBody = err.error;
            this.errorMsg = errorBody?.message || 'Neuspešna prijava. Proverite email i šifru.';
            console.error('Login NEUSPEŠAN! Poruka:', this.errorMsg);
          }
        }
      );
  }

}
