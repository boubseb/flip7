import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { AuthenticationService } from '../../services/authentication/authentification.service';
import { UserService } from '../../services/user/user.service';
import { Router } from '@angular/router';
import { TranslateModule, TranslateService } from '@ngx-translate/core';

@Component({
  selector: 'app-profil',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, TranslateModule],
  templateUrl: './profil.component.html',
  styleUrl: './profil.component.scss'
})
export class ProfilComponent implements OnInit {
  private fb = inject(FormBuilder);
  private authService = inject(AuthenticationService);
  private userService = inject(UserService);
  private router = inject(Router);
  private translate = inject(TranslateService);

  profileForm!: FormGroup;
  passwordForm!: FormGroup;
  successMessage: string = '';
  errorMessage: string = '';
  isLoading: boolean = false;

  ngOnInit(): void {
    this.initForms();
    this.loadUserData();
  }

  initForms(): void {
    this.profileForm = this.fb.group({
      pseudo: ['', [Validators.required, Validators.minLength(2)]],
      email: ['', [Validators.required, Validators.email]]
    });

    this.passwordForm = this.fb.group({
      currentPassword: ['', [Validators.required]],
      newPassword: ['', [Validators.required, Validators.minLength(6)]],
      confirmPassword: ['', [Validators.required, Validators.minLength(6)]]
    }, { validators: this.passwordMatchValidator });
  }

  passwordMatchValidator(form: FormGroup) {
    const password = form.get('newPassword');
    const confirmPassword = form.get('confirmPassword');
    
    if (password && confirmPassword && password.value !== confirmPassword.value) {
      confirmPassword.setErrors({ passwordMismatch: true });
      return { passwordMismatch: true };
    }
    return null;
  }

  loadUserData(): void {
    this.isLoading = true;
    this.userService.getUserProfile().subscribe({
      next: (user) => {
        this.profileForm.patchValue({
          pseudo: user.pseudo || user.firstname || '',
          email: user.email
        });
        this.isLoading = false;
      },
      error: (error) => {
        this.errorMessage = this.translate.instant('profile.errors.loadData') + ': ' + (error.error?.message || error.message);
        this.isLoading = false;
        setTimeout(() => this.errorMessage = '', 5000);
      }
    });
  }

  onUpdateProfile(): void {
    if (this.profileForm.valid) {
      this.isLoading = true;
      const pseudo = this.profileForm.value.pseudo;
      const profileData = {
        firstname: pseudo,
        lastname: pseudo,
        email: this.profileForm.value.email,
        dateOfBirth: '2000-01-01'
      };

      this.userService.updateProfile(profileData).subscribe({
        next: (user) => {
          this.successMessage = this.translate.instant('profile.success.updateProfile');
          this.isLoading = false;
          setTimeout(() => this.successMessage = '', 3000);
        },
        error: (error) => {
          this.errorMessage = this.translate.instant('profile.errors.updateProfile');
          this.isLoading = false;
          setTimeout(() => this.errorMessage = '', 3000);
        }
      });
    }
  }

  onChangePassword(): void {
    if (this.passwordForm.valid) {
      this.isLoading = true;
      const { currentPassword, newPassword } = this.passwordForm.value;

      this.userService.changePassword(currentPassword, newPassword).subscribe({
        next: (response) => {
          this.successMessage = this.translate.instant('profile.success.changePassword');
          this.passwordForm.reset();
          this.isLoading = false;
          setTimeout(() => this.successMessage = '', 3000);
        },
        error: (error) => {
          this.errorMessage = error.error?.message || this.translate.instant('profile.errors.changePassword');
          this.isLoading = false;
          setTimeout(() => this.errorMessage = '', 3000);
        }
      });
    }
  }

  onDeleteAccount(): void {
    const confirmed = confirm(this.translate.instant('profile.confirmDelete'));
    if (confirmed) {
      this.isLoading = true;
      this.userService.deleteAccount().subscribe({
        next: (response) => {
          this.authService.removeToken();
          this.router.navigateByUrl('/');
        },
        error: (error) => {
          this.errorMessage = this.translate.instant('profile.errors.deleteAccount');
          this.isLoading = false;
          setTimeout(() => this.errorMessage = '', 3000);
        }
      });
    }
  }
}
