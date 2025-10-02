import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { AuthenticationService } from '../../services/authentication/authentification.service';
import { UserService } from '../../services/user/user.service';
import { Router } from '@angular/router';

@Component({
  selector: 'app-profil',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './profil.component.html',
  styleUrl: './profil.component.scss'
})
export class ProfilComponent implements OnInit {
  private fb = inject(FormBuilder);
  private authService = inject(AuthenticationService);
  private userService = inject(UserService);
  private router = inject(Router);

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
      firstName: ['', [Validators.required, Validators.minLength(2)]],
      lastName: ['', [Validators.required, Validators.minLength(2)]],
      birthDate: ['', Validators.required],
      email: ['', [Validators.required, Validators.email]]
    });

    this.passwordForm = this.fb.group({
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
    console.log('=== Loading user profile ===');
    this.isLoading = true;
    this.userService.getUserProfile().subscribe({
      next: (user) => {
        console.log('User profile received:', user);
        console.log('User firstname:', user.firstname);
        console.log('User lastname:', user.lastname);
        console.log('User dateOfBirth:', user.dateOfBirth);
        console.log('User email:', user.email);
        
        this.profileForm.patchValue({
          firstName: user.firstname,
          lastName: user.lastname,
          birthDate: user.dateOfBirth,
          email: user.email
        });
        this.isLoading = false;
        console.log('Form patched successfully');
      },
      error: (error) => {
        console.error('=== Error loading user data ===');
        console.error('Error object:', error);
        console.error('Error status:', error.status);
        console.error('Error message:', error.message);
        console.error('Error body:', error.error);
        
        this.errorMessage = 'Erreur lors du chargement des données: ' + (error.error?.message || error.message);
        this.isLoading = false;
        setTimeout(() => this.errorMessage = '', 5000);
      }
    });
  }

  onUpdateProfile(): void {
    if (this.profileForm.valid) {
      this.isLoading = true;
      const profileData = {
        firstname: this.profileForm.value.firstName,
        lastname: this.profileForm.value.lastName,
        email: this.profileForm.value.email,
        dateOfBirth: this.profileForm.value.birthDate
      };

      this.userService.updateProfile(profileData).subscribe({
        next: (user) => {
          this.successMessage = 'Profil mis à jour avec succès !';
          this.isLoading = false;
          setTimeout(() => this.successMessage = '', 3000);
        },
        error: (error) => {
          console.error('Error updating profile:', error);
          this.errorMessage = 'Erreur lors de la mise à jour du profil';
          this.isLoading = false;
          setTimeout(() => this.errorMessage = '', 3000);
        }
      });
    }
  }

  onChangePassword(): void {
    if (this.passwordForm.valid) {
      this.isLoading = true;
      const newPassword = this.passwordForm.value.newPassword;

      this.userService.changePassword(newPassword).subscribe({
        next: (response) => {
          this.successMessage = 'Mot de passe modifié avec succès !';
          this.passwordForm.reset();
          this.isLoading = false;
          setTimeout(() => this.successMessage = '', 3000);
        },
        error: (error) => {
          console.error('Error changing password:', error);
          this.errorMessage = 'Erreur lors du changement de mot de passe';
          this.isLoading = false;
          setTimeout(() => this.errorMessage = '', 3000);
        }
      });
    }
  }

  onDeleteAccount(): void {
    const confirmed = confirm('Êtes-vous sûr de vouloir supprimer votre compte ? Cette action est irréversible.');
    if (confirmed) {
      this.isLoading = true;
      this.userService.deleteAccount().subscribe({
        next: (response) => {
          this.authService.removeToken();
          this.router.navigateByUrl('/');
        },
        error: (error) => {
          console.error('Error deleting account:', error);
          this.errorMessage = 'Erreur lors de la suppression du compte';
          this.isLoading = false;
          setTimeout(() => this.errorMessage = '', 3000);
        }
      });
    }
  }
}
