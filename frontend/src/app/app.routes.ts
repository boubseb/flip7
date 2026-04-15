import { Routes } from '@angular/router';
import { HomePageComponent } from './pages/home-page/home-page.component';
import { LoginPageComponent } from './pages/login-page/login-page.component';
import { RegisterPageComponent } from './pages/register-page/register-page.component';
import { GamePageComponent } from './pages/game-page/game-page.component';
import { RoomPageComponent } from './pages/room-page/room-page.component';
import { AdminPageComponent } from './pages/admin-page/admin-page.component';
import { AuthenticationGuard } from './guards/authentification.guard';
import { AdminGuard } from './guards/admin.guard';

export const routes: Routes = [
  {
    path: 'home',
    component: HomePageComponent,
  },
  {
    path: 'login',
    component: LoginPageComponent,
  },
  {
    path: 'register',
    component: RegisterPageComponent,
  },
  {
    path: 'room',
    component: RoomPageComponent,
  },
  {
    path: 'game/:roomId',
    component: GamePageComponent,
  },
  {
    path: 'admin',
    component: AdminPageComponent,
    canActivate: [AuthenticationGuard, AdminGuard],
  },
  {
    path: '',
    redirectTo: 'home',
    pathMatch: 'full',
  },
];
