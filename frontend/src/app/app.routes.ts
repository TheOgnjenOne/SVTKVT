import { Routes } from '@angular/router';
import {Dashboard} from './homePage/components/dashboard/dashboard';
import {Register} from './auth/components/register/register';
import {RequestPending} from './UserPages/request-pending/request-pending';
import {RegistrationRequests} from './AdminPages/registration-requests/registration-reqests';
import {ProfilePage} from './UserPages/profile-page/profile-page';
import {ProfileTab} from './UserPages/profile-page/profile-tab/profile-tab';
import {SettingsTab} from './UserPages/profile-page/settings-tab/settings-tab';
import {LocationsComponent} from './Location/location-page/location-page';
import {EventPage} from './Event/event-page/event-page';
import {LocationEventPage} from './Event/location-event-page/location-event-page';
import {LocationAnalyticsComponent} from './Location/location-analytics/location-analytics';
import {AuthGuard} from './guards/auth.guard';
import {RoleGuard} from './guards/role.guard';

export const routes: Routes = [
  {
    path: '',
    redirectTo: 'login',
    pathMatch: 'full'
  },

  // JAVNE RUTE
  {
    path: 'login',
    loadComponent: () =>
      import('../app/auth/components/login/login').then(m => m.Login)
  },
  {
    path: 'register',
    component: Register
  },
  {
    path: 'request-pending',
    component: RequestPending
  },

  // --- RUTE ZA ULOGOVANE KORISNIKE  ---
  {
    path: 'dashboard',
    component: Dashboard,
    canActivate: [AuthGuard]
  },
  {
    path: 'profile',
    component: ProfilePage,
    canActivate: [AuthGuard],
    children: [
      {path: 'details', component:ProfileTab },
      {path: 'settings', component: SettingsTab},
    ]
  },
  {
    path: 'locations',
    component: LocationsComponent,
    canActivate: [AuthGuard]
  },
  {
    path: 'events',
    component: EventPage,
    canActivate: [AuthGuard]
  },
  {
    path: 'events/:locationId',
    component: LocationEventPage,
    canActivate: [AuthGuard]
  },

  {
    path: 'admin/registration-requests',
    component: RegistrationRequests,
    canActivate: [AuthGuard, RoleGuard],
    data: { roles: ['ADMIN'] }
  },
  {
    path: 'analytics',
    component: LocationAnalyticsComponent,
    canActivate: [AuthGuard, RoleGuard],
    data: { roles: ['ADMIN', 'MANAGER'] }
  }
];
