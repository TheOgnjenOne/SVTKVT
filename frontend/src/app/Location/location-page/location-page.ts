import { Component, OnInit, OnDestroy } from '@angular/core';
import { LocationService } from '../../services/location/location-service';
import {Location, LocationListDTO} from '../../models/location.model';
import { NgClass, NgForOf, NgIf } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuthService, CurrentUser } from '../../services/auth/auth';
import { Subscription } from 'rxjs';
import { AdminService, ManagerInfo } from '../../services/admin/admin';
import {Router} from '@angular/router';
import {LocationReview} from '../location-review/location-review';
import {AllReviewsModalComponent} from '../../componentParts/all-reviews-modal/all-reviews-modal';

interface LocationUI extends Location {
  showDetails: boolean;
  reviewCount?: number;
  isManagedByCurrentUser?: boolean;
}

interface AvailableUser {
  id: number;
  email: string;
}

@Component({
  selector: 'app-location-page',
  templateUrl: './location-page.html',
  styleUrls: ['./location-page.css'],
  standalone: true,
  imports: [NgIf, FormsModule, NgForOf, NgClass, LocationReview, AllReviewsModalComponent]
})
export class LocationsComponent implements OnInit, OnDestroy {

  locations: LocationUI[] = [];
  loading = true;
  errorMessage = '';
  currentUser: CurrentUser | null = null;
  private userSubscription: Subscription | undefined;
  showAddForm = false;
  showEditForm = false;

  managedLocationIds: number[] = [];

  showDeleteConfirmModal = false;
  deletingLocationId: number | undefined;

  showManagerModal = false;
  managingLocationId: number | undefined;
  activeManagers: ManagerInfo[] = [];
  availableUsers: AvailableUser[] = [];

  showAllReviewsModal = false;
  reviewsLocationId: number | undefined;
  reviewsLocationName: string = '';
  isReviewsLocationManaged: boolean = false;

  managerAssignment: { userId: number | null, startDate: string, endDate: string | null } = {
    userId: null,
    startDate: new Date().toISOString().substring(0, 10),
    endDate: null
  };

  editingLocation: LocationUI | null = null;
  selectedFile: File | null = null;
  newLocation: Partial<Location> = {
    name: '',
    address: '',
    type: '',
    description: ''
  };
  searchName: string = '';
  searchAddress: string = '';
  searchType: string = '';

  constructor(
    private locationService: LocationService,
    private authService: AuthService,
    private adminService: AdminService,
    private router: Router
  ) {
  }

  ngOnInit(): void {
    this.loadCurrentUser();
    this.loadLocations();
    this.loadManagedLocations();
  }

  ngOnDestroy(): void {
    if (this.userSubscription) {
      this.userSubscription.unsubscribe();
    }
  }

  getLocationImage(loc: LocationUI): string {
    return loc.image?.path ? 'http://localhost:8080/uploads/' + loc.image.path : '/defaultLoc.png';
  }
  onImageError(event: Event) {
    const img = event.target as HTMLImageElement;
    img.src = '/defaultLoc.png';
  }
  protected loadLocations(): void {
    this.loading = true;

    const isUserManager = this.currentUser?.role === 'MANAGER';

    const detailsState: { [id: number]: boolean } = {};
    this.locations.forEach(loc => {
      if (loc.id !== undefined && loc.id !== null) {
        detailsState[loc.id] = loc.showDetails;
      }
    });

    this.locationService.getAll().subscribe({
      next: (data: LocationListDTO[]) => {
        this.locations = data.map(loc => {

          const isManagedByCurrentUser = isUserManager ? this.isManagerOfLocation(loc.id) : false;
          const finalReviewCount = loc.reviewCount;

          let showDetailsStatus = false;

          if (loc.id !== undefined && loc.id !== null) {
            showDetailsStatus = detailsState[loc.id] || false;
          }
          return {
            ...loc,
            showDetails: showDetailsStatus,
            isManagedByCurrentUser: isManagedByCurrentUser,
            reviewCount: finalReviewCount
          };
        });
        this.loading = false;
      },
      error: (err) => {
        console.error('Greška pri učitavanju lokacija:', err);
        this.errorMessage = 'Greška pri učitavanju lokacija';
        this.loading = false;
      }
    });
  }

  private loadCurrentUser(): void {
    this.userSubscription = this.authService.currentUser$.subscribe(user => {
      this.currentUser = user;
    });
  }

  private loadManagedLocations(): void {
    if (this.currentUser && this.currentUser.role === 'MANAGER') {
      this.locationService.getMyManagedLocations().subscribe({
        next: (ids: number[]) => {
          this.managedLocationIds = ids;
        },
        error: (err) => {
          console.error('Greška pri dohvatanju menadžerskih ID-eva:', err);
          this.managedLocationIds = [];
        }
      });
    }
  }

  isManagerOfLocation(locationId: number | undefined): boolean {
    if (!this.currentUser || this.currentUser.role !== 'MANAGER' || locationId === undefined) {
      return false;
    }
    return this.managedLocationIds.includes(locationId);
  }

  addLocation(): void {
    if (!this.newLocation.name || !this.newLocation.address || !this.newLocation.type || !this.newLocation.description) {
      alert('Popuni sva obavezna polja!');
      return;
    }

    const formData = new FormData();
    formData.append('location', JSON.stringify(this.newLocation));

    if (this.selectedFile) {
      formData.append('image', this.selectedFile);
    }

    this.locationService.create(formData).subscribe({
      next: (savedLocation) => {
        this.locations.push({...savedLocation, showDetails: false});
        this.showAddForm = false;
        this.newLocation = {name: '', address: '', type: '', description: ''};
        this.selectedFile = null;
      },
      error: (err) => {
        console.error('Greška pri dodavanju lokacije:', err);
        alert('Došlo je do greške pri dodavanju lokacije.');
      }
    });
  }


  openEditModal(loc: LocationUI): void {
    this.editingLocation = {...loc};
    this.selectedFile = null;
    this.showEditForm = true;
  }

  saveLocationEdit(): void {
    if (!this.editingLocation || !this.editingLocation.id) return;
    if (!this.editingLocation.name || !this.editingLocation.address || !this.editingLocation.type || !this.editingLocation.description) {
      alert('Popuni sva obavezna polja!');
      return;
    }

    const formData = new FormData();
    formData.append('location', JSON.stringify({
      name: this.editingLocation.name,
      address: this.editingLocation.address,
      type: this.editingLocation.type,
      description: this.editingLocation.description
    }));

    if (this.selectedFile) {
      formData.append('image', this.selectedFile);
    }

    this.locationService.update(this.editingLocation.id, formData).subscribe({
      next: (updatedLoc) => {
        const index = this.locations.findIndex(l => l.id === updatedLoc.id);
        if (index !== -1) {
          this.locations[index] = {...updatedLoc, showDetails: this.locations[index].showDetails};
        }
        this.showEditForm = false;
        this.editingLocation = null;
        this.selectedFile = null;
      },
      error: (err) => {
        console.error('Greška pri izmeni lokacije:', err);
        alert('Došlo je do greške pri izmeni lokacije.');
      }
    });
  }

  openManagerModal(locationId: number | undefined): void {
    if (locationId) {
      this.managingLocationId = locationId;
      this.managerAssignment = {
        userId: null,
        startDate: new Date().toISOString().substring(0, 10),
        endDate: null
      };

      this.loadAvailableUsers();
      this.loadActiveManagers(locationId);
      this.showManagerModal = true;
    }
  }

  loadAvailableUsers(): void {
    this.adminService.loadAvailableUsers().subscribe({
      next: (users) => {
        this.availableUsers = users.filter(user =>
          user.email !== this.currentUser?.email
        );
      },
      error: (err) => {
        console.error('Greška pri dohvatanju dostupnih korisnika:', err);
        this.availableUsers = [];
      }
    });
  }


  loadActiveManagers(locationId: number): void {
    this.adminService.getActiveManagers(locationId).subscribe({
      next: (data) => {
        this.activeManagers = data;
      },
      error: (err) => {
        console.error('Greška pri dohvatanju menadžera:', err);
        this.activeManagers = [];
        alert('Greška pri dohvatanju liste aktivnih menadžera.');
      }
    });
  }

  closeManagerModal(): void {
    this.showManagerModal = false;
    this.managingLocationId = undefined;
    this.managerAssignment = { userId: null, startDate: new Date().toISOString().substring(0, 10), endDate: null };
    this.activeManagers = [];
    this.availableUsers = [];
  }

  assignManager(): void {
    if (!this.managingLocationId || !this.managerAssignment.userId || !this.managerAssignment.startDate) {
      alert('Korisnik i datum početka su obavezni!');
      return;
    }

    const assignmentData = {
      userId: this.managerAssignment.userId,
      locationId: this.managingLocationId,
      startDate: this.managerAssignment.startDate,
      endDate: this.managerAssignment.endDate || undefined
    };

    this.adminService.assignManager(assignmentData).subscribe({
      next: (response) => {

        this.loadActiveManagers(this.managingLocationId!);
        this.managerAssignment.userId = null;
      },
      error: (err) => {
        console.error('Greška pri dodeli menadžera:', err);
        alert(err.error?.message || 'Došlo je do greške pri dodeli menadžera.');
      }
    });
  }

  unassignManager(userId: number): void {
    if (!this.managingLocationId || !userId) {
      alert('Korisnik ID i lokacija su obavezni za uklanjanje.');
      return;
    }

    if (!confirm(`Potvrdi uklanjanje menadžera ${userId}?`)) {
      return;
    }

    const unassignmentData = {
      userId: userId,
      locationId: this.managingLocationId
    };

    this.adminService.unassignManager(unassignmentData).subscribe({
      next: (response) => {
        this.loadActiveManagers(this.managingLocationId!);
      },
      error: (err) => {
        console.error('Greška pri uklanjanju menadžera:', err);
        alert(err.error?.message || 'Došlo je do greške pri uklanjanju menadžera.');
      }
    });
  }

  confirmDelete(id: number | undefined): void {
    if (id) {
      this.deletingLocationId = id;
      this.showDeleteConfirmModal = true;
    }
  }

  deleteLocation(): void {
    if (!this.deletingLocationId) return;

    this.locationService.delete(this.deletingLocationId).subscribe({
      next: () => {
        this.locations = this.locations.filter(loc => loc.id !== this.deletingLocationId);
        this.showDeleteConfirmModal = false;
        this.deletingLocationId = undefined;
      },
      error: (err) => {
        console.error('Greška pri brisanju lokacije:', err);
        alert('Došlo je do greške pri brisanju lokacije.');
        this.showDeleteConfirmModal = false;
        this.deletingLocationId = undefined;
      }
    });
  }

  cancelDelete(): void {
    this.showDeleteConfirmModal = false;
    this.deletingLocationId = undefined;
  }

  onFileSelected(event: Event) {
    const target = event.target as HTMLInputElement;
    if (target.files && target.files.length > 0) {
      this.selectedFile = target.files[0];
    }
  }

  viewEvents(locationId: number | undefined): void {
    if (locationId) {
      this.router.navigate(['/events', locationId]);
    }
  }

  openReviewsModal(locationId: number | undefined): void {
    if (!locationId) return;

    const location = this.locations.find(loc => loc.id === locationId);

    if (location) {
      this.reviewsLocationId = locationId;
      this.reviewsLocationName = location.name;
      this.isReviewsLocationManaged = this.isManagerOfLocation(locationId);
      this.showAllReviewsModal = true;
    }
  }

  closeReviewsModal(): void {
    this.showAllReviewsModal = false;
    this.reviewsLocationId = undefined;
    this.reviewsLocationName = '';
    this.isReviewsLocationManaged = false;
  }

  handleReviewChange(): void {
    if (this.reviewsLocationId) {
      this.locationService.getById(this.reviewsLocationId).subscribe(updatedLocation => {
        const index = this.locations.findIndex(loc => loc.id === this.reviewsLocationId);
        if (index !== -1) {
          const oldShowDetails = this.locations[index].showDetails;
          this.locations[index] = { ...this.locations[index], ...updatedLocation, showDetails: oldShowDetails };
        }
      });
    }
  }

  get filteredLocations(): LocationUI[] {
    let locations = this.locations;

    const nameTerm = this.searchName.toLowerCase();
    const addressTerm = this.searchAddress.toLowerCase();
    const typeTerm = this.searchType.toLowerCase();

    if (nameTerm) {
      locations = locations.filter(loc => loc.name.toLowerCase().includes(nameTerm));
    }

    if (addressTerm) {
      locations = locations.filter(loc => loc.address.toLowerCase().includes(addressTerm));
    }

    if (typeTerm) {
      locations = locations.filter(loc => loc.type.toLowerCase().includes(typeTerm));
    }

    return locations;
  }
}
