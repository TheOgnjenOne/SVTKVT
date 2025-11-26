
import { Component, Input, Output, EventEmitter, forwardRef } from '@angular/core'; // Dodajemo forwardRef
import { NgFor, NgIf, DatePipe, NgClass } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { CommentDTO } from '../../models/comment.model';
import { CurrentUser } from '../../services/auth/auth';


@Component({
  selector: 'app-comment-tree',
  templateUrl: './comment-tree.html',
  standalone: true,
  imports: [
    NgFor,
    NgIf,
    DatePipe,
    NgClass,
    FormsModule,
    forwardRef(() => CommentTreeComponent)
  ]
})
export class CommentTreeComponent {

  @Input() comments: CommentDTO[] = [];
  @Input() reviewId!: number;
  @Input() currentUser: CurrentUser | null = null;
  @Input() isManagerOrAdmin: boolean = false;

  @Input() replyingToReviewId: number | null = null;
  @Input() replyingToCommentId: number | null = null;

  @Input() newCommentText: string = '';
  @Output() newCommentTextChange = new EventEmitter<string>();

  @Output() toggleReplyEvent = new EventEmitter<{ reviewId: number, commentId: number | null }>();
  @Output() submitCommentEvent = new EventEmitter<void>();

  onNewCommentTextChange(value: string): void {
    this.newCommentTextChange.emit(value);
  }
  isAdminComment(comment: CommentDTO): boolean {
    return comment.userEmail.toLowerCase().includes('admin');
  }


}
