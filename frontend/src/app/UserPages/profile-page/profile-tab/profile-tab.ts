import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { NgIf, NgForOf } from '@angular/common';
import { UserService, UserData, UserUpdateData, UserReview, ManagedLocation } from '../../../services/user/user';
import {Router, RouterLink} from '@angular/router';

@Component({
  selector: 'app-profile',
  templateUrl: './profile-tab.html',
  standalone: true,
  imports: [
    FormsModule,
    NgForOf,
    NgIf,
    RouterLink
  ],
  styleUrls: ['./profile-tab.css']
})
export class ProfileTab implements OnInit {
  userData: UserData = {
    id: 0,
    email: '',
    name: '',
    phoneNumber: '',
    birthday: '',
    address: '',
    city: '',
    role: 'USER',
    profileImageId: null
  };

  isEditing = false;
  isSaving = false;
  isLoading = true;
  selectedImage: File | null = null;
  managedLocations: ManagedLocation[] = [];
  userReviews: UserReview[] = [];
  isUploading = false;

  constructor(private userService: UserService, private router: Router) {}

  ngOnInit() {
    this.loadData();
  }

  get currentProfileImageUrl(): string {
    if (this.userData.profileImageId) {
      return `http://localhost:8080/api/images/${this.userData.profileImageId}`;
    }
    return '/default-avatar.png';
  }

  loadData() {
    this.isLoading = true;

    this.userService.getUserProfile().subscribe({
      next: (data) => {
        this.userData = data;
        this.isLoading = false;
      },
      error: (err) => {
        console.error('Greška pri učitavanju profila', err);
        this.isLoading = false;
      }
    });

    this.userService.getUserReviews().subscribe({
      next: (reviews) => {
        console.log('UTISCI USPEŠNO PRIMLJENI:', reviews);
        this.userReviews = reviews;
      },
      error: (err) => console.error('Greška pri učitavanju utisaka', err)
    });

    this.userService.getManagedLocations().subscribe({
      next: (locations) => {
        this.managedLocations = locations;
      },
      error: (err) => console.error('Greška pri učitavanju lokacija', err)
    });
  }

  startEditing() {
    this.isEditing = true;
  }

  cancelEditing() {
    this.isEditing = false;
    this.loadData();
  }

  saveDetails() {
    this.isSaving = true;

    const updateData: UserUpdateData = {
      name: this.userData.name,
      phoneNumber: this.userData.phoneNumber,
      birthday: this.userData.birthday,
      address: this.userData.address,
      city: this.userData.city
    };

    this.userService.updateProfile(updateData).subscribe({
      next: (response) => {
        console.log('Profil uspešno sačuvan', response);
        this.isSaving = false;
        this.isEditing = false;
      },
      error: (err) => {
        console.error('Greška pri čuvanju profila', err);
        this.isSaving = false;
      }
    });
  }

  handleImageSelect(event: any) {
    const file = event.target.files[0];
    if (file) {
      this.selectedImage = file;
    }
  }

  async uploadImage() {
    if (!this.selectedImage) return;

    this.isUploading = true;

    this.userService.uploadProfileImage(this.selectedImage).subscribe({
      next: (response) => {
        console.log('Slika uspešno uploadovana', response);


        this.loadData();

        this.selectedImage = null;
        this.isUploading = false;
      },
      error: (err) => {
        console.error('Greška pri uploadu slike', err);
        this.isUploading = false;
      }
    });
  }

  changePassword() {
    this.router.navigate(['/profile/settings']);
  }

  manageLocation(locationId: number) {
    console.log('Upravljanje lokacijom:', locationId);
  }
}
