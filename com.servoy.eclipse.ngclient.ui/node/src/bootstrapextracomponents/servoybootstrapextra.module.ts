
import { NgModule } from '@angular/core';
import { ServoyBootstrapExtraBreadcrumbs } from './breadcrumbs/breadcrumbs';
import { ServoyBootstrapExtraButtonsGroup } from './buttonsgroup/buttonsgroup';
import { ServoyBootstrapExtraBadge } from './badge/badge';
import { ServoyBootstrapExtraRating } from './rating/rating';
import { ServoyBootstrapExtraProgressBar } from './progressbar/progressbar';
import { ServoyBootstrapExtraCarousel, Slide } from './carousel/carousel';
import { MenuItem, ServoyBootstrapExtraNavbar, SvyAttributes } from './navbar/navbar';
import { CommonModule } from '@angular/common';
import { ServoyPublicModule } from '../ngclient/servoy_public.module';
import { SabloModule } from '../sablo/sablo.module';
import { NgbModule }  from '@ng-bootstrap/ng-bootstrap';
import { FormsModule } from '@angular/forms';
import { SpecTypesService } from '../sablo/spectypes.service';

@NgModule({
    declarations: [
      ServoyBootstrapExtraBreadcrumbs,
      ServoyBootstrapExtraNavbar,
      ServoyBootstrapExtraCarousel,
      ServoyBootstrapExtraBadge,
      ServoyBootstrapExtraButtonsGroup,
      ServoyBootstrapExtraRating,
      ServoyBootstrapExtraProgressBar,
      SvyAttributes
    ],
    providers: [],
    imports: [
      CommonModule,
      ServoyPublicModule,
      SabloModule,
      NgbModule,
      FormsModule
    ],
    exports: [
        ServoyBootstrapExtraBreadcrumbs,
        ServoyBootstrapExtraNavbar,
        ServoyBootstrapExtraCarousel,
        ServoyBootstrapExtraBadge,
        ServoyBootstrapExtraButtonsGroup,
        ServoyBootstrapExtraRating,
        ServoyBootstrapExtraProgressBar,
        SvyAttributes
      ]
})
export class ServoyBootstrapExtraComponentsModule {
      constructor( specTypesService: SpecTypesService ) {
         specTypesService.registerType('bootstrapextracomponents-navbar.menuItem', MenuItem);
         specTypesService.registerType('bootstrapextracomponents-carousel.slide', Slide);
    }
}
