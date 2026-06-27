import { Component, OnInit, OnDestroy } from '@angular/core';
import { NgIf, NgForOf, DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subscription } from 'rxjs';
import {
  SearchService,
  LocationSearchRequest,
  LocationSearchResult
} from '../../services/search/search-service';
import { LocationService } from '../../services/location/location-service';
import { AuthService, CurrentUser } from '../../services/auth/auth';

@Component({
  selector: 'app-location-search',
  standalone: true,
  imports: [NgIf, NgForOf, DecimalPipe, FormsModule],
  templateUrl: './location-search.html',
  styleUrls: ['./location-search.css']
})
export class LocationSearchComponent implements OnInit, OnDestroy {

  request: LocationSearchRequest = this.emptyRequest();

  results: LocationSearchResult[] = [];
  total = 0;
  loading = false;
  searched = false;
  errorMessage = '';
  resultsTitle = 'Rezultati pretrage';

  // PDF upload (admin / menadžer)
  currentUser: CurrentUser | null = null;
  manageableLocations: { id: number; name: string }[] = [];
  selectedLocationId: number | null = null;
  selectedPdf: File | null = null;
  uploadMessage = '';
  uploadError = '';

  private sub?: Subscription;

  constructor(
    private searchService: SearchService,
    private locationService: LocationService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    this.sub = this.authService.currentUser$.subscribe(user => {
      this.currentUser = user;
      this.loadManageableLocations();
    });
    this.doSearch(); // početno: prikaži sva mesta (match_all)
  }

  ngOnDestroy(): void {
    this.sub?.unsubscribe();
  }

  get isManagerOrAdmin(): boolean {
    return this.currentUser?.role === 'ADMIN' || this.currentUser?.role === 'MANAGER';
  }

  private emptyRequest(): LocationSearchRequest {
    return {
      naziv: '', opis: '', pdfOpis: '', tipMesta: '',
      operator: 'AND',
      reviewCountMin: null, reviewCountMax: null,
      avgNastupMin: null, avgNastupMax: null,
      avgZvukSvetloMin: null, avgZvukSvetloMax: null,
      avgProstorMin: null, avgProstorMax: null,
      avgUkupnoMin: null, avgUkupnoMax: null,
      sortBy: '', sortDir: 'asc'
    };
  }

  doSearch(): void {
    this.loading = true;
    this.errorMessage = '';
    this.searched = true;
    this.resultsTitle = 'Rezultati pretrage';

    const payload: LocationSearchRequest = { ...this.request, page: 0, size: 50 };
    if (!payload.sortBy) {
      payload.sortBy = null;
      payload.sortDir = null;
    }

    this.searchService.search(payload).subscribe({
      next: (res) => {
        this.results = res.results;
        this.total = res.total;
        this.loading = false;
      },
      error: () => {
        this.errorMessage = 'Greška pri pretrazi. Da li je Elasticsearch pokrenut?';
        this.results = [];
        this.total = 0;
        this.loading = false;
      }
    });
  }

  resetForm(): void {
    this.request = this.emptyRequest();
    this.doSearch();
  }

  moreLikeThis(result: LocationSearchResult): void {
    this.loading = true;
    this.errorMessage = '';
    this.searched = true;
    this.searchService.moreLikeThis(result.id, 10).subscribe({
      next: (res) => {
        this.results = res.results;
        this.total = res.total;
        this.resultsTitle = `Slična mesta kao "${result.naziv}"`;
        this.loading = false;
        window.scrollTo({ top: 0, behavior: 'smooth' });
      },
      error: () => {
        this.errorMessage = 'Greška pri traženju sličnih mesta.';
        this.loading = false;
      }
    });
  }

  downloadPdf(result: LocationSearchResult): void {
    this.searchService.downloadPdf(result.id).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `mesto-${result.id}.pdf`;
        a.click();
        window.URL.revokeObjectURL(url);
      },
      error: () => alert('PDF nije dostupan za ovo mesto.')
    });
  }

  imageUrl(imageId: number | null): string {
    return imageId ? `http://localhost:8080/api/images/${imageId}` : '/defaultLoc.png';
  }

  onImageError(event: Event): void {
    (event.target as HTMLImageElement).src = '/defaultLoc.png';
  }

  // --- PDF upload (admin / menadžer) ---

  loadManageableLocations(): void {
    if (!this.isManagerOrAdmin) {
      this.manageableLocations = [];
      return;
    }
    this.locationService.getAll().subscribe({
      next: (locs) => {
        if (this.currentUser?.role === 'ADMIN') {
          this.manageableLocations = locs
            .filter(l => l.id != null)
            .map(l => ({ id: l.id!, name: l.name }));
        } else {
          this.locationService.getMyManagedLocations().subscribe({
            next: (ids) => {
              const managed = new Set(ids);
              this.manageableLocations = locs
                .filter(l => l.id != null && managed.has(l.id))
                .map(l => ({ id: l.id!, name: l.name }));
            }
          });
        }
      }
    });
  }

  onPdfSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.selectedPdf = input.files && input.files.length ? input.files[0] : null;
  }

  uploadPdf(): void {
    this.uploadMessage = '';
    this.uploadError = '';
    if (!this.selectedLocationId) {
      this.uploadError = 'Izaberite mesto.';
      return;
    }
    if (!this.selectedPdf) {
      this.uploadError = 'Izaberite PDF fajl.';
      return;
    }
    this.searchService.uploadPdf(this.selectedLocationId, this.selectedPdf).subscribe({
      next: () => {
        this.uploadMessage = 'PDF uspešno postavljen i indeksiran (pdfOpis).';
        this.selectedPdf = null;
      },
      error: () => this.uploadError = 'Greška pri postavljanju PDF-a.'
    });
  }
}
