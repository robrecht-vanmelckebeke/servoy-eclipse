import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { SabloModule } from '../../sablo/sablo.module'
import { ServoyPublicModule } from '../../ngclient/servoy_public.module'


import { FormattingService, LocaleService, I18NProvider} from '../../ngclient/servoy_public'
import { By, BrowserModule } from '@angular/platform-browser';
import { ServoyDefaultCalendar } from "./calendar";
import { DateTimeAdapter, OwlDateTimeIntl, OwlDateTimeModule, OwlNativeDateTimeModule } from '@danielmoncada/angular-datetime-picker';
import { Renderer2 } from '@angular/core';
import { SabloService } from '../../sablo/sablo.service';
import { FormsModule } from '@angular/forms';
let moment = require('moment');

describe("ServoyDefaultCalendar", () => {
    let component: ServoyDefaultCalendar;
    let fixture: ComponentFixture<ServoyDefaultCalendar>;
    let i18nProvider;
    //let dateTimeAdapter;
   // let owlDateTimeIntl;

    beforeEach(async(() => {  
        i18nProvider = jasmine.createSpyObj("I18NProvider",["getI18NMessages"]);
        const promise = Promise.resolve({});
        i18nProvider.getI18NMessages.and.returnValue(promise);

        (<any>window).moment = moment;
        
        TestBed.configureTestingModule({
            declarations: [ServoyDefaultCalendar],
            imports: [BrowserModule, SabloModule, ServoyPublicModule, OwlDateTimeModule, FormsModule, OwlNativeDateTimeModule],
            providers: [Renderer2, FormattingService, { provide: LocaleService, useValue: {getLocale: () => 'en' } }, { provide: I18NProvider, useValue: i18nProvider },
                OwlDateTimeIntl, SabloService]
        }).compileComponents();
    }));

    beforeEach(() => {
        fixture = TestBed.createComponent(ServoyDefaultCalendar);
        component = fixture.componentInstance;
        component.servoyApi =  jasmine.createSpyObj("ServoyApi", ["getMarkupId","trustAsHtml", 'startEdit']);
        fixture.detectChanges();
      });

    it('should create', () => {
        expect(component).toBeTruthy();
    });

    it('should have called servoyApi.getMarkupId', () => {
        expect( component.servoyApi.getMarkupId ).toHaveBeenCalled();
    });
    
    it('should be ok', async() => {
        component.inputElementRef.nativeElement.value = 'test';
        fixture.detectChanges();
        fixture.whenStable().then(() => {
            let input = fixture.debugElement.query(By.css('input'));
            let el = input.nativeElement;
            expect(el.value).toBe('test');
            el.value = 'someValue';
            el.dispatchEvent(new Event('input'));
            expect(component.inputElementRef.nativeElement.value).toBe('someValue');
        })
    }); 
}); 

