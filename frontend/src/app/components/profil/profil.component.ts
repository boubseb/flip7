import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { AuthenticationService } from '../../services/authentication/authentification.service';

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

  profileForm!: FormGroup;
  passwordForm!: FormGroup;
  successMessage: string = '';
  errorMessage: string = '';

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
    // TODO: Load user data from backend
    // For now, using mock data
    this.profileForm.patchValue({
      firstName: 'John',
      lastName: 'Doe',
      birthDate: '1990-01-01',
      email: 'john.doe@example.com'
    });
  }

  onUpdateProfile(): void {
    if (this.profileForm.valid) {
      console.log('Updating profile:', this.profileForm.value);
      // TODO: Call backend API to update profile
      this.successMessage = 'Profil mis à jour avec succès !';
      setTimeout(() => this.successMessage = '', 3000);
    }
  }

  onChangePassword(): void {
    if (this.passwordForm.valid) {
      console.log('Changing password');
      // TODO: Call backend API to change password
      this.successMessage = 'Mot de passe modifié avec succès !';
      this.passwordForm.reset();
      setTimeout(() => this.successMessage = '', 3000);
    }
  }

  onDeleteAccount(): void {
    const confirmed = confirm('Êtes-vous sûr de vouloir supprimer votre compte ? Cette action est irréversible.');
    if (confirmed) {
      console.log('Deleting account');
      // TODO: Call backend API to delete account
      this.authService.removeToken();
      window.location.href = '/';
    }
  }
}
