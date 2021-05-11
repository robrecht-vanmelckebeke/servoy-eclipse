import { NgModule } from '@angular/core';

import { TooltipDirective } from './tooltip/tooltip.directive';
import { TooltipService } from './tooltip/tooltip.service';
import { DecimalkeyconverterDirective } from './directives/decimalkeyconverter.directive';
import { MnemonicletterFilterPipe, NotNullOrEmptyPipe, HtmlFilterPipe, TrustAsHtmlPipe} from './format/pipes';
import { FormatFilterPipe } from './format/format.pipe';
import { EmptyValueFilterPipe } from './format/emptyvalue.pipe';
import { StartEditDirective } from './directives/startedit.directive';
import { ImageMediaIdDirective } from './directives/imagemediaid.directive';
import {AutosaveDirective } from './directives/autosave.directive';
import { UploadDirective } from './directives/upload.directive';
import { FormatDirective } from './format/formatcontrolvalueaccessor.directive';
import { FormattingService } from './format/formatting.service';
import { ComponentContributor } from './basecomponent';
import { SabloTabseq } from './directives/sablotabseq.directive';
import { WindowRefService } from './services/windowref.service';
import { SpecTypesService } from './spectypes.service';
import { LoggerFactory } from './logger.service';

@NgModule({
    declarations: [ TooltipDirective,
                    MnemonicletterFilterPipe,
                    NotNullOrEmptyPipe,
                    HtmlFilterPipe,
                    FormatDirective,
                    DecimalkeyconverterDirective,
                    FormatFilterPipe,
                    EmptyValueFilterPipe,
                    StartEditDirective,
                    ImageMediaIdDirective,
                    AutosaveDirective,
                    UploadDirective,
                    SabloTabseq,
                    TrustAsHtmlPipe
                  ],
    imports: [],
    exports: [TooltipDirective,
              MnemonicletterFilterPipe,
              NotNullOrEmptyPipe,
              HtmlFilterPipe,
              FormatDirective,
              DecimalkeyconverterDirective,
              FormatFilterPipe,
              EmptyValueFilterPipe,
              StartEditDirective,
              ImageMediaIdDirective,
              AutosaveDirective,
              UploadDirective,
              SabloTabseq,
              TrustAsHtmlPipe
             ],
    providers: [ TooltipService, FormattingService, ComponentContributor, SpecTypesService, WindowRefService, LoggerFactory ]
})
export class ServoyPublicModule { }
