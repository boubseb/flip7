import { Component, OnInit, OnDestroy, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, Router, NavigationEnd } from '@angular/router';
import { Subscription, filter } from 'rxjs';

@Component({
  selector: 'app-footer',
  standalone: true,
  imports: [RouterLink, CommonModule],
  templateUrl: './footer.component.html',
  styleUrl: './footer.component.scss',
})
export class FooterComponent implements OnInit, OnDestroy {
  isGamePage: boolean = false;
  private routerSubscription?: Subscription;

  // Inputs for game state
  @Input() isMyTurn: boolean = false;
  @Input() isEliminated: boolean = false;

  // Outputs for game actions
  @Output() onHitClick = new EventEmitter<void>();
  @Output() onStopClick = new EventEmitter<void>();

  constructor(private router: Router) {}

  ngOnInit(): void {
    this.checkRoute(this.router.url);

    this.routerSubscription = this.router.events
      .pipe(filter(event => event instanceof NavigationEnd))
      .subscribe((event: any) => {
        this.checkRoute(event.url);
      });
  }

  ngOnDestroy(): void {
    if (this.routerSubscription) {
      this.routerSubscription.unsubscribe();
    }
  }

  private checkRoute(url: string): void {
    this.isGamePage = url.includes('/game/');
  }

  onHit(): void {
    console.log('🎴 Footer: Hit clicked');
    this.onHitClick.emit();
  }

  onStop(): void {
    console.log('✋ Footer: Stop clicked');
    this.onStopClick.emit();
  }
}
