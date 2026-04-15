import { Component, inject } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { User } from '../../models/user/user';
import { AuthenticationService } from '../../services/authentication/authentification.service';
import { TranslateModule } from '@ngx-translate/core';

@Component({
  selector: 'app-register-page',
  standalone: true,
  imports: [ReactiveFormsModule, TranslateModule, RouterLink],
  templateUrl: './register-page.component.html',
  styleUrl: './register-page.component.scss'
})
export class RegisterPageComponent {
  registerForm: FormGroup;
  user?: User;
  router = inject(Router);

  authentificationService = inject(AuthenticationService);

  constructor(private fb: FormBuilder) {
    this.registerForm = this.fb.group({
      pseudo: ['', [Validators.required]],
      mailAddress: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(4)]],
    });
  }

  // passwordValidator(control: FormControl): { [key: string]: boolean } | null {
  //   const value = control.value;
  //   if (!/[A-Z]/.test(value) || !/[0-9]/.test(value) || !/[!@#$%^&*(),.?":{}|<>]/.test(value)) {
  //     return { 'weakPassword': true };
  //   }
  //   return null;
  // }


  onSubmit(): void {
    if (this.registerForm.valid) {
      const pseudo = this.registerForm.value.pseudo;
      this.user = new User(
        pseudo,
        pseudo,
        pseudo,
        this.registerForm.value.password,
        this.registerForm.value.mailAddress,
        '2000-01-01',
      );
      this.authentificationService.register(this.user).subscribe(() => {
        this.router.navigateByUrl('/');
      });
    }
  }
}
