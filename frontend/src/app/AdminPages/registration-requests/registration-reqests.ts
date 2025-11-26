import { Component, OnInit } from '@angular/core';
import { AdminService, AccountRequest } from '../../services/admin/admin';
import { FormsModule } from '@angular/forms';
import { NgForOf, NgIf } from '@angular/common';

@Component({
  selector: 'app-requests',
  templateUrl: 'registration-requests.html',
  imports: [
    FormsModule,
    NgIf,
    NgForOf
  ],
  styleUrls: ['registration-requests.css']
})
export class RegistrationRequests implements OnInit {

  requests: (AccountRequest & { showRejectInput?: boolean; reason?: string; isProcessing?: boolean;  errorMessage?: string;
  })[] = [];
  isLoading = false;

  constructor(private adminService: AdminService) {}

  ngOnInit(): void {
    this.loadRequests();
  }

  loadRequests() {
    this.isLoading = true;
    this.adminService.getPendingRequests().subscribe({
      next: res => {
        this.requests = res;
        this.isLoading = false;
      },
      error: err => {
        console.error('Greška pri učitavanju zahteva:', err);
        this.isLoading = false;
      }
    });
  }

  approve(req: AccountRequest & { isProcessing?: boolean }) {
    req.isProcessing = true;
    this.requests = this.requests.filter(r => r.id !== req.id);

    this.adminService.approveRequest(req.id).subscribe({
      next: () => console.log('Request approved.'),
      error: err => {
        console.error('Greška pri prihvatanju zahteva:', err);
        this.requests.push(req);
      }
    });
  }

  confirmReject(req: AccountRequest & { reason?: string; isProcessing?: boolean; errorMessage?: string }) {
    if (!req.reason || !req.reason.trim()) {
      req.errorMessage = 'Molimo unesite razlog odbijanja.';
      return;
    }

    req.errorMessage = undefined;
    req.isProcessing = true;
    this.requests = this.requests.filter(r => r.id !== req.id);

    this.adminService.rejectRequest(req.id, req.reason).subscribe({
      next: () => console.log('Request rejected.'),
      error: err => {
        console.error('Greška pri odbijanju zahteva:', err);
        this.requests.push(req);
        req.errorMessage = 'Greška pri odbijanju zahteva.';
      }
    });
  }
}
