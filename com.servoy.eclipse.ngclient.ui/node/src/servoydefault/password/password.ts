import {Component, Renderer2, ChangeDetectorRef, ChangeDetectionStrategy} from '@angular/core';
import {FormattingService} from '../../ngclient/servoy_public';
import {ServoyDefaultBaseField} from '../basefield';
@Component({
  selector: 'servoydefault-password',
  templateUrl: './password.html',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class ServoyDefaultPassword extends ServoyDefaultBaseField<HTMLInputElement> {
  constructor(renderer: Renderer2, cdRef: ChangeDetectorRef, formattingService: FormattingService) {
    super(renderer, cdRef, formattingService);
  }
}
