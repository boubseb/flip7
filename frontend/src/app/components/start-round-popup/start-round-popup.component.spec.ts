import { ComponentFixture, TestBed } from '@angular/core/testing';

import { StartRoundPopupComponent } from './start-round-popup.component';

describe('StartRoundPopupComponent', () => {
  let component: StartRoundPopupComponent;
  let fixture: ComponentFixture<StartRoundPopupComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [StartRoundPopupComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(StartRoundPopupComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should show correct message for current player', () => {
    component.isCurrentPlayer = true;
    component.roundNumber = 3;
    
    expect(component.getMessage()).toContain('C\'est à vous de commencer le round 3');
  });

  it('should show correct message for waiting player', () => {
    component.isCurrentPlayer = false;
    component.playerUsername = 'John';
    component.roundNumber = 2;
    
    expect(component.getMessage()).toContain('En attente de John');
    expect(component.getMessage()).toContain('round 2');
  });

  it('should emit start round when current player clicks button', () => {
    spyOn(component.onStartRound, 'emit');
    component.isCurrentPlayer = true;
    
    component.startRound();
    
    expect(component.onStartRound.emit).toHaveBeenCalled();
  });

  it('should not emit start round when not current player', () => {
    spyOn(component.onStartRound, 'emit');
    component.isCurrentPlayer = false;
    
    component.startRound();
    
    expect(component.onStartRound.emit).not.toHaveBeenCalled();
  });

  it('should show game icon for current player', () => {
    component.isCurrentPlayer = true;
    expect(component.getIcon()).toBe('🎮');
  });

  it('should show waiting icon for other players', () => {
    component.isCurrentPlayer = false;
    expect(component.getIcon()).toBe('⏳');
  });
});
