import { ComponentFixture, TestBed } from '@angular/core/testing';

import { RevocationToolsPage } from './revocation-tools-page';

describe('RevocationToolsPage', () => {
  let component: RevocationToolsPage;
  let fixture: ComponentFixture<RevocationToolsPage>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RevocationToolsPage]
    })
    .compileComponents();

    fixture = TestBed.createComponent(RevocationToolsPage);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
