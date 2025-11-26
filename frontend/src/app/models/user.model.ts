import { UserRole } from './user-role';
import { Image } from './image.model';
import { LocationManager } from './location-manager.model';
import { Review } from './review.model';
import { Comment } from './comment.model';

export interface User {
  id?: number;
  email: string;
  password?: string;
  name: string;
  phoneNumber?: string;
  birthday?: string;
  address?: string;
  city?: string;
  createdAt?: string;
  role: UserRole;
  profileImage?: Image;
  managedLocations?: LocationManager[];
  reviews?: Review[];
  comments?: Comment[];
}
