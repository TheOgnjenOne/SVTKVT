import { Review } from './review.model';
import { User } from './user.model';

export interface Comment {
  id?: number;
  text: string;
  createdAt?: string;
  review?: Review;
  user?: User;
  parentComment?: Comment;
  replies?: Comment[];
}
export interface CommentDTO {
  id: number;
  text: string;
  createdAt: string;
  userEmail: string;
  userId: number;
  replies: CommentDTO[];
  isManagerReply: boolean;
}
export interface CommentPayload {
  reviewId: number;
  parentCommentId?: number;
  text: string;
}
