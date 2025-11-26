import { Component, Input, Output, EventEmitter, OnInit } from '@angular/core';
import { NgIf, NgFor, DatePipe, NgClass, DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ReviewService } from '../../services/rewiev/review-service';
import { AuthService, CurrentUser } from '../../services/auth/auth';
import { ReviewResponseDTO, ReviewDisplay } from '../../models/review.model';
import { CommentTreeComponent } from '../comment-tree/comment-tree';
import { CommentPayload } from '../../models/comment.model';

@Component({
  selector: 'app-all-reviews-modal',
  templateUrl: './all-reviews-modal.html',
  styleUrls: ['./all-reviews-modal.css'],
  standalone: true,
  imports: [NgIf, NgFor, DatePipe, FormsModule, NgClass, DecimalPipe, CommentTreeComponent]
})
export class AllReviewsModalComponent implements OnInit {

  @Input() locationId!: number;
  @Input() locationName!: string;
  @Output() closeModal = new EventEmitter<void>();
  @Input() isManagedByCurrentUser: boolean = false;
  @Output() reviewChanged = new EventEmitter<void>();

  reviews: ReviewDisplay[] = [];
  loading = true;
  error = '';

  currentUser: CurrentUser | null = null;
  isAdmin: boolean = false;
  isManagerOrAdmin: boolean = false;

  replyingToReviewId: number | null = null;
  replyingToCommentId: number | null = null;
  newCommentText: string = '';
  isSubmittingComment: boolean = false;
  reviewToDeleteId: number | null = null;
  isDeleting: boolean = false;

  sortMode: 'date-desc' | 'date-asc' | 'rating-desc' | 'rating-asc' = 'date-desc';

  constructor(
    private reviewService: ReviewService,
    private authService: AuthService
  ) { }

  ngOnInit(): void {
    this.authService.currentUser$.subscribe(user => {
      this.currentUser = user;
      this.isAdmin = user?.role === 'ADMIN';
      this.isManagerOrAdmin = this.isAdmin || this.isManagedByCurrentUser;
    });

    if (this.locationId) {
      this.loadAllReviews();
    } else {
      this.error = 'Greška: ID lokacije nije dostupan.';
      this.loading = false;
    }
  }

  onNewCommentTextChange(text: string): void {
    this.newCommentText = text;
  }

  loadAllReviews(): void {
    this.loading = true;
    this.error = '';

    this.reviewService.getAllReviewsByLocation(this.locationId).subscribe({
      next: (data: ReviewResponseDTO[]) => {
        let reviewsData = data as ReviewDisplay[];

        if (!this.isManagerOrAdmin) {
          reviewsData = reviewsData.filter(r => !r.isHidden);
        }

        this.reviews = reviewsData;
        this.loading = false;

        this.sortReviews();
      },
      error: (err) => {
        this.error = 'Greška pri učitavanju recenzija.';
        this.loading = false;
        console.error('API Greška prilikom dohvatanja recenzija:', err);
      }
    });
  }

  sortReviews(): void {
    this.reviews.sort((a, b) => {
      switch (this.sortMode) {
        case 'date-desc':
          return new Date(b.submissionDate).getTime() - new Date(a.submissionDate).getTime();
        case 'date-asc':
          return new Date(a.submissionDate).getTime() - new Date(b.submissionDate).getTime();
        case 'rating-desc':
          return (b.overallRating || 0) - (a.overallRating || 0);
        case 'rating-asc':
          return (a.overallRating || 0) - (b.overallRating || 0);
        default:
          return 0;
      }
    });
  }


  toggleReply(reviewId: number, commentId: number | null = null): void {
    if (this.replyingToReviewId === reviewId && this.replyingToCommentId === commentId) {
      this.replyingToReviewId = null;
      this.replyingToCommentId = null;
      this.newCommentText = '';
    } else {
      this.replyingToReviewId = reviewId;
      this.replyingToCommentId = commentId;
      this.newCommentText = '';
    }
  }


  submitComment(): void {
    if (!this.newCommentText || !this.replyingToReviewId) {
      return;
    }

    this.isSubmittingComment = true;

    const payload: CommentPayload = {
      reviewId: this.replyingToReviewId,
      parentCommentId: this.replyingToCommentId || undefined,
      text: this.newCommentText
    };

    setTimeout(() => {
      this.reviewService.addComment(payload).subscribe({
        next: (updatedReview: ReviewResponseDTO) => {
          const index = this.reviews.findIndex(r => r.id === updatedReview.id);
          if (index !== -1) {
            this.reviews[index] = updatedReview as ReviewDisplay;
            this.reviews = [...this.reviews];
          }

          this.replyingToReviewId = null;
          this.replyingToCommentId = null;
          this.newCommentText = '';

          this.isSubmittingComment = false;
        },
        error: (err) => {
          const errorMessage = err.error?.message || 'Nepoznata greška.';
          alert(`Greška: ${errorMessage}`);
          console.error('Greška pri dodavanju komentara:', err);

          this.isSubmittingComment = false;
        }
      });
    }, 0);
  }


  initiateDelete(reviewId: number): void {
    this.reviewToDeleteId = reviewId;
  }

  cancelDelete(): void {
    this.reviewToDeleteId = null;
  }

  confirmDelete(): void {
    if (this.reviewToDeleteId === null) {
      return;
    }

    this.isDeleting = true;

    setTimeout(() => {
      this.reviewService.deleteReview(this.reviewToDeleteId!).subscribe({
        next: () => {
          this.reviews = this.reviews.filter(r => r.id !== this.reviewToDeleteId);
          this.reviewChanged.emit();

          this.reviewToDeleteId = null;
          this.isDeleting = false;
        },
        error: (err) => {
          alert('Greška pri uklanjanju recenzije: ' + (err.error?.message || 'Nepoznata greška.'));
          console.error('Greška pri brisanju recenzije:', err);
          this.reviewToDeleteId = null;
          this.isDeleting = false;
        }
      });
    }, 500);
  }



  toggleHideReview(reviewId: number, isCurrentlyHidden: boolean): void {
    if (!this.isManagerOrAdmin) return;

    const newHiddenState = !isCurrentlyHidden;

    this.reviewService.toggleReviewVisibility(reviewId, newHiddenState).subscribe({
      next: (updatedReview) => {
        alert(`Recenzija je uspešno ${newHiddenState ? 'sakrivena' : 'prikazana'}. Ocena se i dalje računa.`);
        const index = this.reviews.findIndex(r => r.id === updatedReview.id);
        if (index !== -1) {
          this.reviews[index].isHidden = updatedReview.isHidden;
          this.reviewChanged.emit();
        }
        this.sortReviews();
      },
      error: (err) => {
        alert('Greška pri promeni vidljivosti: ' + (err.error?.message || 'Nepoznata greška.'));
        console.error('Greška pri promeni vidljivosti:', err);
      }
    });
  }
}
