
import { Component, Input, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { Router, RouterLink,  } from '@angular/router';
import { Login } from '../../models/login/login.model';
import { AuthenticationService } from '../../services/authentication/authentification.service';
import { FooterComponent } from '../../components/footer/footer.component';
import { TranslateModule } from '@ngx-translate/core';



@Component({
  selector: 'app-popup',
  standalone: true,
  imports: [ MatButtonModule, FormsModule, RouterLink, FooterComponent, TranslateModule],
  templateUrl: './login-page.component.html',
  styleUrl: './login-page.component.scss'
})
export class LoginPageComponent {
  login:Login=new Login("","");

  authenticationService=inject(AuthenticationService)
  router=inject(Router)

  constructor(){}



  onLogin(username:string,password:string):void {
    this.authenticationService.login(username,password).subscribe(
      (response: any) => {
        if (response.access_token) {
          this.authenticationService.setToken(response.access_token);
          this.router.navigateByUrl('/'); 
        } else {
          console.error('Login failed: No access token in response');
        }
      },
      (error) => {
        console.error('Login failed:', error);
      },
    );
  }
  }
  

