import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { NgIf } from '@angular/common';
import { UserService, ChangePassword } from '../../../services/user/user';

@Component({
  selector: 'app-settings-tab',
  standalone: true,
  imports: [
    FormsModule,
    NgIf
  ],
  templateUrl: './settings-tab.html',
  styleUrls: ['./settings-tab.css']
})
export class SettingsTab {

  currentPassword = '';
  newPassword = '';
  confirmPassword = '';
  errorMessage = '';
  successMessage = '';
  isProcessing = false;

  constructor(private userService: UserService) {}

  changePassword() {
    this.errorMessage = '';
    this.successMessage = '';
    this.isProcessing = true;

    if (this.newPassword !== this.confirmPassword) {
      this.errorMessage = 'Nova lozinka i potvrda se ne poklapaju!';
      this.isProcessing = false;
      return;
    }

    if (this.newPassword.length < 6) {
      this.errorMessage = 'Nova lozinka mora imati najmanje 6 karaktera!';
      this.isProcessing = false;
      return;
    }

    const payload: ChangePassword = {
      currentPassword: this.currentPassword,
      newPassword: this.newPassword
    };

    this.userService.changePassword(payload).subscribe({
      next: (res: any) => {
        this.successMessage = res?.message || 'Lozinka uspešno promenjena!';
        this.currentPassword = '';
        this.newPassword = '';
        this.confirmPassword = '';
        this.isProcessing = false;
      },
      error: (err: any) => {
        this.errorMessage = err.error?.message || 'Došlo je do greške!';
        this.isProcessing = false;
      }
    });
  }
}
