import { ServoyBootstrapBaseLabel } from "../bts_baselabel";
import { Component, Input, Renderer2, ChangeDetectorRef } from "@angular/core";

@Component({
    selector: 'bootstrapcomponents-label',
    templateUrl: './label.html',
    styleUrls: ['./label.scss'] 
})
export class ServoyBootstrapLabel extends ServoyBootstrapBaseLabel {

    @Input() labelFor;
    @Input() styleClassExpression;

    constructor(renderer: Renderer2, cdRef: ChangeDetectorRef) {
        super(renderer, cdRef);
    }
    
    private getDataTarget(event): any {
        const dataTarget = event.target.closest("data-target");
        if (dataTarget && dataTarget[0]) {
            return dataTarget[0].getAttribute("data-target");
        }
        return null;
    }

    attachHandlers() {
        if ( this.onActionMethodID ) {
            if (this.onDoubleClickMethodID) {
                const innerThis: ServoyBootstrapLabel = this;
                this.renderer.listen( this.getNativeElement(), 'click', e => {
                    if (innerThis.timeoutID) {
                        window.clearTimeout(innerThis.timeoutID);
                        innerThis.timeoutID = null;
                        // double click, do nothing
                    } else {
                        innerThis.timeoutID = window.setTimeout(function() {
                            innerThis.timeoutID = null;
                            innerThis.onActionMethodID(e, innerThis.getDataTarget(e));
                        }, 250); }
                 });
            } else {
                this.renderer.listen( this.getNativeElement(), 'click', e => this.onActionMethodID(e, this.getDataTarget(e)));
            }
        }
        if ( this.onRightClickMethodID ) {
          this.renderer.listen( this.getNativeElement(), 'contextmenu', e => { 
              this.onRightClickMethodID(e, this.getDataTarget(e)); return false; });
        }
        if ( this.onDoubleClickMethodID ) {
            this.renderer.listen( this.elementRef.nativeElement, 'dblclick', ( e ) => {
                this.onDoubleClickMethodID(e, this.getDataTarget(e));
            } );
        }
    }
}