import { Component, OnInit, ViewChildren, QueryList } from '@angular/core';
import { NgModel, FormsModule } from '@angular/forms';
import { NgIf } from '@angular/common';
import {AuthService} from '../../../services/auth/auth';
import {Router} from '@angular/router';

@Component({
  selector: 'app-register',
  templateUrl: './register.html',
  imports: [FormsModule, NgIf],
  styleUrls: ['./register.css']
})
export class Register implements OnInit {

  registerModel = {
    email: '',
    password: '',
    address: ''
  };

  @ViewChildren(NgModel) ngModels!: QueryList<NgModel>;

  errorMsg: string | null = null;
  successMsg: string | null = null;

  constructor(private authService: AuthService, private router: Router) {
  }

  ngOnInit() {
  }

  isEmailValid(): string | null {
    if (!this.registerModel.email) return 'Email je obavezan.';
    if (!/^\S+@\S+\.\S+$/.test(this.registerModel.email)) return 'Unesite validan email.';
    return null;
  }

  isPasswordValid(): string | null {
    if (!this.registerModel.password) return 'Lozinka je obavezna.';
    if (this.registerModel.password.length < 6) return 'Lozinka mora imati najmanje 6 karaktera.';
    return null;
  }


  //SUBMIT
  onRegisterSubmit() {
    this.ngModels.forEach(ctrl => ctrl.control.markAsTouched());
    const errors = [
      this.isEmailValid(),
      this.isPasswordValid()
    ].filter(e => e != null);
    if (errors.length > 0) {
      console.log('Greške pri registraciji:', errors);
      return;
    }

    this.authService.clearPendingRegistrationEmail();
    this.authService.setPendingRegistrationEmail(this.registerModel.email);

    this.authService.register(this.registerModel).subscribe({
      next: (response) => {
        this.router.navigate(['/request-pending']);
      },
      error: () => {
        this.errorMsg = 'Greška pri komunikaciji sa serverom.';
      }
    });


  }
}
