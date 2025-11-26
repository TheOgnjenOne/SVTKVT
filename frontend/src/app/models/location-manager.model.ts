import { Location } from './location.model';
import { User } from './user.model';

export interface LocationManager {
  id?: {
    locationId: number;
    userId: number;
  };
  location?: Location;
  user?: User;
  startDate: string;
  endDate?: string;
}
