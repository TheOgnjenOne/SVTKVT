import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth/auth';
import {NgClass, NgIf} from '@angular/common';

@Component({
  selector: 'app-request-pending',
  templateUrl: './request-pending.html',
  imports: [
    NgClass,
    NgIf
  ],
  styleUrls: ['./request-pending.css']
})
export class RequestPending implements OnInit {

  status: 'PENDING' | 'ACCEPTED' | 'REJECTED' | 'NOT_FOUND' | null = null;
  message: string = '';
  rejectionReason: string | null = null;

  constructor(private router: Router, private authService: AuthService) {}

  ngOnInit(): void {
    const email = this.authService.getPendingRegistrationEmail();
    if (!email) {
      this.status = 'NOT_FOUND';
      this.message = 'Status zahteva nije poznat.';
      return;
    }

    this.authService.getRegistrationStatus(email).subscribe({
      next: (response) => {
        this.status = response.status as 'PENDING' | 'ACCEPTED' | 'REJECTED' | 'NOT_FOUND';
        this.rejectionReason = response.reason || null;

        switch (this.status) {
          case 'PENDING':
            this.message = 'Zahtev je u obradi. Molimo vas za strpljenje.';
            break;
          case 'ACCEPTED':
            this.message = 'Vaš zahtev je prihvaćen! Možete se prijaviti.';
            break;
          case 'REJECTED':
            this.message = 'Vaš zahtev je odbijen.';
            break;
          default:
            this.message = 'Status zahteva nije poznat.';
        }
      },
      error: () => {
        this.status = 'NOT_FOUND';
        this.message = 'Greška pri proveri statusa zahteva.';
      }
    });
  }
  retryRegistration() {
      const email = this.authService.getPendingRegistrationEmail();
      if (!email) return;

      this.authService.resubmit(email).subscribe({
        next: (res) => {
          this.message = res.message;
          this.status = 'PENDING';
        },
        error: (err) => {
          this.message = err.error?.message || 'Greška pri ponovnom slanju zahteva.';
        }
      });
    }

  checkStatus() {
    this.ngOnInit();
  }

  login() {
    this.router.navigate(['/login']);
  }
}
