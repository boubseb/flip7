import { ComponentFixture, TestBed } from '@angular/core/testing';

import { PlayerSelectorComponent } from './player-selector.component';

describe('PlayerSelectorComponent', () => {
  let component: PlayerSelectorComponent;
  let fixture: ComponentFixture<PlayerSelectorComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PlayerSelectorComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(PlayerSelectorComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should filter eligible players (PLAYING status only)', () => {
    component.players = [
      { userId: '1', username: 'Player1', status: 'PLAYING' },
      { userId: '2', username: 'Player2', status: 'STOPPED' },
      { userId: '3', username: 'Player3', status: 'PLAYING' },
      { userId: '4', username: 'Player4', status: 'ELIMINATED' }
    ];
    
    expect(component.getEligiblePlayersCount()).toBe(2);
  });

  it('should emit player selection for eligible player', () => {
    spyOn(component.onPlayerSelected, 'emit');
    
    component.players = [
      { userId: '1', username: 'Player1', status: 'PLAYING' }
    ];
    
    component.selectPlayer('1');
    
    expect(component.onPlayerSelected.emit).toHaveBeenCalledWith('1');
  });

  it('should not emit player selection for non-eligible player', () => {
    spyOn(component.onPlayerSelected, 'emit');
    
    component.players = [
      { userId: '1', username: 'Player1', status: 'ELIMINATED' }
    ];
    
    component.selectPlayer('1');
    
    expect(component.onPlayerSelected.emit).not.toHaveBeenCalled();
  });

  it('should emit cancel event', () => {
    spyOn(component.onCancel, 'emit');
    
    component.cancel();
    
    expect(component.onCancel.emit).toHaveBeenCalled();
  });

  it('should return correct icon for card type', () => {
    component.cardType = 'STOP';
    expect(component.getCardIcon()).toBe('🛑');
    
    component.cardType = 'DRAW_THREE';
    expect(component.getCardIcon()).toBe('➕3️⃣');
  });

  it('should return correct color for card type', () => {
    component.cardType = 'STOP';
    expect(component.getCardColor()).toBe('#ef4444');
    
    component.cardType = 'DRAW_THREE';
    expect(component.getCardColor()).toBe('#f97316');
  });
});
