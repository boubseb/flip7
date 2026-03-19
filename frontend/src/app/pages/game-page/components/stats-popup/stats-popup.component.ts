import { Component, Input, Output, EventEmitter } from '@angular/core';
import { TranslateModule } from '@ngx-translate/core';

@Component({
  selector: 'app-stats-popup',
  standalone: true,
  imports: [TranslateModule],
  templateUrl: './stats-popup.component.html',
  styleUrls: ['./stats-popup.component.scss']
})
export class StatsPopupComponent {
  @Input() probability: number | null = null;
  @Output() close = new EventEmitter<void>();

  formatProbability(value: number): string {
    return value.toLocaleString(undefined, { minimumFractionDigits: 1, maximumFractionDigits: 2 });
  }
}
