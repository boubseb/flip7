import { Component, inject} from '@angular/core';
import { Observable } from 'rxjs';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { AuthenticationService } from '../../services/authentication/authentification.service';


@Component({
  selector: 'app-home-page',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './home-page.component.html',
  styleUrl: './home-page.component.scss'
})
export class HomePageComponent {

   authenticationService = inject(AuthenticationService);

  isLogin$: Observable<Boolean> = this.authenticationService.isLogin$;


}
