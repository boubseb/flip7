import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { UserProfile } from '../../models/user/user-profile';

@Injectable({
  providedIn: 'root'
})
export class UserService {
  private http = inject(HttpClient);
  private url = environment.apiUrl;

  getUserProfile(): Observable<UserProfile> {
    return this.http.get<UserProfile>(`${this.url}/profil`);
  }

  updateProfile(profileData: {
    firstname: string;
    lastname: string;
    email: string;
    dateOfBirth: string;
  }): Observable<UserProfile> {
    return this.http.post<UserProfile>(`${this.url}/updateProfile`, profileData);
  }

  changePassword(currentPassword: string, newPassword: string): Observable<{ message: string }> {
    return this.http.post<{ message: string }>(`${this.url}/changePassword`, {
      currentPassword,
      newPassword
    });
  }

  deleteAccount(): Observable<{ message: string }> {
    return this.http.delete<{ message: string }>(`${this.url}/deleteAccount`);
  }
}
