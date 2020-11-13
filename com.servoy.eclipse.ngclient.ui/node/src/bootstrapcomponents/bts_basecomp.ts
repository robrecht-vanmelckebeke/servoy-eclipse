
import { ServoyBaseComponent, PropertyUtils } from '../ngclient/servoy_public';
import { Directive, Input, Renderer2, SimpleChanges, AfterViewInit, OnChanges, ChangeDetectorRef } from '@angular/core';

@Directive()
export class ServoyBootstrapBaseComponent extends ServoyBaseComponent {

    @Input() onActionMethodID;
    @Input() onRightClickMethodID;
    @Input() onDoubleClickMethodID;

    @Input() enabled;
    @Input() size;
    @Input() styleClass;
    @Input() tabSeq;
    @Input() text;
    @Input() toolTipText;
    @Input() visible;

    timeoutID: number;

    constructor(protected readonly renderer: Renderer2, protected cdRef: ChangeDetectorRef) {
        super(renderer, cdRef);
    }

    svyOnInit() {
        super.svyOnInit();
        this.attachHandlers();
    }

    svyOnChanges( changes: SimpleChanges ) {
        if (changes) {
            for ( const property of Object.keys(changes) ) {
                const change = changes[property];
                switch ( property ) {
                case 'enabled':
                    if ( change.currentValue )
                        this.renderer.removeAttribute(this.getFocusElement(),  'disabled' );
                    else
                        this.renderer.setAttribute(this.getFocusElement(),  'disabled', 'disabled' );
                    break;
                case 'styleClass':
                    if (change.previousValue) {
                        const array = change.previousValue.split(' ');
                        array.forEach(element => this.renderer.removeClass(this.getStyleClassElement(), element ));
                    }
                    if ( change.currentValue) {
                        const array = change.currentValue.split(' ');
                        array.forEach(element => this.renderer.addClass(this.getStyleClassElement(), element ));
                    }
                    break;
                }
            }
        }
        super.svyOnChanges(changes);
    }

    getFocusElement(): any {
        return this.getNativeElement();
    }

    getStyleClassElement(): any {
        return this.getNativeElement();
    }

    public requestFocus() {
        this.getFocusElement().focus();
    }

    protected attachHandlers() {
        if ( this.onActionMethodID ) {
            if (this.onDoubleClickMethodID) {
                const innerThis: ServoyBootstrapBaseComponent = this;
                this.renderer.listen( this.getNativeElement(), 'click', e => {
                    if (innerThis.timeoutID) {
                        window.clearTimeout(innerThis.timeoutID);
                        innerThis.timeoutID = null;
                        // double click, do nothing
                    } else {
                        innerThis.timeoutID = window.setTimeout(function() {
                            innerThis.timeoutID = null;
                            innerThis.onActionMethodID( e );
                        }, 250); }
                 });
            } else {
                if (this.getNativeElement().tagName == 'TEXTAREA' || this.getNativeElement().type == 'text') {
                    this.renderer.listen( this.getNativeElement(), 'keydown', e => { if (e.keyCode == 13) this.onActionMethodID( e ); });
                } else {
                    this.renderer.listen( this.getNativeElement(), 'click', e => this.onActionMethodID( e ));
                }

            }
        }
        if ( this.onRightClickMethodID ) {
          this.renderer.listen( this.getNativeElement(), 'contextmenu', e => { this.onRightClickMethodID( e ); return false; });
        }
    }
    public getScrollX(): number {
        return this.getNativeElement().scrollLeft;
    }

    public getScrollY(): number {
        return this.getNativeElement().scrollTop;
    }

    public setScroll(x: number, y: number) {
        this.getNativeElement().scrollLeft = x;
        this.getNativeElement().scrollTop = y;
    }

    needsScrollbarInformation(): boolean {
        return true;
    }

}
