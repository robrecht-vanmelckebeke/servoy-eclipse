import {Component,Input, ChangeDetectorRef, Renderer2, SimpleChanges, ChangeDetectionStrategy} from '@angular/core';
import {ServoyDefaultBaseComponent} from '../basecomponent';
@Component({
  selector: 'servoydefault-rectangle',
  templateUrl: './rectangle.html',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class ServoyDefaultRectangle extends ServoyDefaultBaseComponent<HTMLDivElement> {
    @Input() lineSize;
    @Input() roundedRadius;
    @Input() shapeType;

    constructor(renderer: Renderer2, cdRef: ChangeDetectorRef) {
        super(renderer, cdRef);
    }

    svyOnChanges( changes: SimpleChanges ) {

        super.svyOnChanges(changes);

        for ( const property in changes ) {
            const change = changes[property];
            switch ( property ) {
            case 'lineSize':
                this.renderer.setStyle(this.getNativeElement(), 'borderWidth', change.currentValue + 'px');
                if (!changes['borderType'] || !changes['borderType'].currentValue) {
                    this.renderer.setStyle(this.getNativeElement(), 'borderStyle', 'solid');
                }
                break;
            case 'foreground':
                this.renderer.setStyle(this.getNativeElement(), 'borderColor', change.currentValue ? change.currentValue : '#000000');
                break;
            case 'roundedRadius':
                this.renderer.setStyle(this.getNativeElement(), 'borderRadius', change.currentValue/2 + 'px');
                break;
            case 'shapeType':
                if (change.currentValue == 3 && changes['size']) {
                    this.renderer.setStyle(this.getNativeElement(), 'borderRadius', changes['size'].currentValue.width/2 + 'px');
                }
                break;
            }
        }
    }

}
