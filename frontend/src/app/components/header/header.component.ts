import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { RulesComponent } from '../rules/rules.component';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [
    CommonModule,
    RulesComponent,
    RouterLink
],
  templateUrl: './header.component.html',
  styleUrl: './header.component.scss',
})
export class HeaderComponent {

  constructor() { }

  isRules: boolean = false;
  toggleIsRules() {
    this.isRules = !this.isRules;
  }

}


