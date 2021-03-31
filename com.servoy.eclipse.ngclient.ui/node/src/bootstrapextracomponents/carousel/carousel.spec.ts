import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { ServoyBootstrapExtraCarousel } from './carousel';

describe('ServoyBootstrapExtraCarousel', () => {
  let component: ServoyBootstrapExtraCarousel;
  let fixture: ComponentFixture<ServoyBootstrapExtraCarousel>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ ServoyBootstrapExtraCarousel ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ServoyBootstrapExtraCarousel);
    component = fixture.componentInstance;
    component.servoyApi =  jasmine.createSpyObj('ServoyApi', ['getMarkupId','trustAsHtml','registerComponent','unRegisterComponent', 'isInAbsoluteLayout']);
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
