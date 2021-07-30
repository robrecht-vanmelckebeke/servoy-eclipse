import { Component, OnInit, Inject, ViewChild, ElementRef, Renderer2 } from '@angular/core';
import { DOCUMENT } from '@angular/common';
import { EditorSessionService, ISelectionChangedListener } from '../services/editorsession.service';

@Component({
    selector: 'selection-decorators',
    templateUrl: './mouseselection.component.html',
    styleUrls: ['./mouseselection.component.css']
})
// this should include lasso and all selection logic from mouseselection.js and dragselection.js
export class MouseSelectionComponent implements OnInit, ISelectionChangedListener {

    @ViewChild('lasso', { static: false }) lassoRef: ElementRef;

    nodes: Array<SelectionNode> = new Array<SelectionNode>();
    contentInit: boolean = false;
    topAdjust: number;
    leftAdjust: number;
    contentRect: DOMRect;
    lassostarted: boolean = false;

    mousedownpoint: Point;

    constructor(protected readonly editorSession: EditorSessionService, @Inject(DOCUMENT) private doc: Document, protected readonly renderer: Renderer2) {
        this.editorSession.addSelectionChangedListener(this);
    }

    ngOnInit(): void {
        this.editorSession.requestSelection();
    }

    selectionChanged(selection: Array<string>): void {
        if (this.contentInit) {
            this.createNodes(selection);
        }
        else {
            this.doc.querySelector('iframe').addEventListener('load', () => {
                this.calculateAdjustToMainRelativeLocation();
                this.createNodes(selection)
            });
        }
    }

    private createNodes(selection: Array<string>) {
        this.contentInit = true;
        let newNodes = new Array<SelectionNode>();
        if (selection.length > 0) {
            let iframe = this.doc.querySelector('iframe');
            let elements = iframe.contentWindow.document.querySelectorAll('[svy-id]');
            if (elements.length == 0) {
                setTimeout(() => this.createNodes(selection), 400);
                return;
            }
            Array.from(elements).forEach(node => {
                if (selection.indexOf(node.getAttribute('svy-id')) >= 0) {
                    let position = node.getBoundingClientRect();
                    let style = {
                        height: position.height + 'px',
                        width: position.width + 'px',
                        top: position.top + this.topAdjust + 'px',
                        left: position.left + this.leftAdjust + 'px',
                        display: 'block'
                    };
                    newNodes.push({
                        style: style
                    })
                }
            });
        }

        this.nodes = newNodes;
    }

    private calculateAdjustToMainRelativeLocation() {
        if (!this.topAdjust) {
            let content = this.doc.querySelector('.content-area') as HTMLElement;
            this.contentRect = content.getBoundingClientRect()
            content.addEventListener('mousedown', (event) => this.onMouseDown(event));
            content.addEventListener('mouseup', (event) => this.onMouseUp(event));
            content.addEventListener('mousemove', (event) => this.onMouseMove(event));
            let computedStyle = window.getComputedStyle(content, null)
            this.topAdjust = parseInt(computedStyle.getPropertyValue('padding-left').replace("px", ""));
            this.leftAdjust = parseInt(computedStyle.getPropertyValue('padding-top').replace("px", ""))
        }
    }

    private onMouseDown(event: MouseEvent) {
        this.lassostarted = false;
        let point = { x: event.pageX, y: event.pageY };
        let frameElem = this.doc.querySelector('iframe');
        let frameRect = frameElem.getBoundingClientRect();
        point.x = point.x - frameRect.left;
        point.y = point.y - frameRect.top;

        let elements = frameElem.contentWindow.document.querySelectorAll('[svy-id]');
        let found = Array.from(elements).find((node) => {
            let position = node.getBoundingClientRect();
            if (position.x <= point.x && position.x + position.width >= point.x && position.y <= point.y && position.y + position.height >= point.y) {
                let id = node.getAttribute('svy-id');
                let selection = this.editorSession.getSelection();
                let newNode = {
                    style: {
                        height: position.height + 'px',
                        width: position.width + 'px',
                        top: position.top + this.topAdjust + 'px',
                        left: position.left + this.leftAdjust + 'px',
                        display: 'block'
                    }
                };
                if (event.ctrlKey || event.metaKey) {
                    let index = selection.indexOf(id);
                    if (index >= 0) {
                        this.nodes.splice(index, 1);
                        selection.splice(index, 1);
                    }
                    else {
                        this.nodes.push(newNode);
                        selection.push(id);
                    }
                }
                else {
                    let newNodes = new Array<SelectionNode>();
                    newNodes.push(newNode);
                    this.nodes = newNodes;
                    selection = [id];
                }
                this.editorSession.setSelection(selection);
                return node;
            }
        });
        if (!found) {
            this.nodes = [];
            this.editorSession.setSelection([]);

            this.renderer.setStyle(this.lassoRef.nativeElement, 'left', event.pageX - this.contentRect.left + 'px');
            this.renderer.setStyle(this.lassoRef.nativeElement, 'top', event.pageY - this.contentRect.top + 'px');
            this.renderer.setStyle(this.lassoRef.nativeElement, 'width', '0px');
            this.renderer.setStyle(this.lassoRef.nativeElement, 'height', '0px');

            this.lassostarted = true;
            this.mousedownpoint = { x: event.pageX, y: event.pageY };
        }
    }

    private onMouseUp(event: MouseEvent) {
        if (this.lassostarted && this.mousedownpoint.x != event.pageX && this.mousedownpoint.y != event.pageY) {
            let frameElem = this.doc.querySelector('iframe');
            let frameRect = frameElem.getBoundingClientRect();
            let elements = frameElem.contentWindow.document.querySelectorAll('[svy-id]');
            let newNodes = new Array<SelectionNode>();
            let newSelection = new Array<string>();
            Array.from(elements).forEach((node) => {
                let position = node.getBoundingClientRect();
                if (this.rectangleContainsPoint({ x: event.pageX, y: event.pageY }, { x: this.mousedownpoint.x, y: this.mousedownpoint.y }, { x: position.x + frameRect.x, y: position.y + frameRect.y }) ||
                    this.rectangleContainsPoint({ x: event.pageX, y: event.pageY }, { x: this.mousedownpoint.x, y: this.mousedownpoint.y }, { x: position.x + frameRect.x + position.width, y: position.y + frameRect.y }) ||
                    this.rectangleContainsPoint({ x: event.pageX, y: event.pageY }, { x: this.mousedownpoint.x, y: this.mousedownpoint.y }, { x: position.x + frameRect.x, y: position.y + frameRect.y + position.height }) ||
                    this.rectangleContainsPoint({ x: event.pageX, y: event.pageY }, { x: this.mousedownpoint.x, y: this.mousedownpoint.y }, { x: position.x + frameRect.x + position.width, y: position.y + frameRect.y + position.height })) {
                    newNodes.push({
                        style: {
                            height: position.height + 'px',
                            width: position.width + 'px',
                            top: position.top + this.topAdjust + 'px',
                            left: position.left + this.leftAdjust + 'px',
                            display: 'block'
                        }
                    });
                    newSelection.push(node.getAttribute('svy-id'))
                }
            });
            this.nodes = newNodes;
            this.editorSession.setSelection(newSelection);
        }
        this.lassostarted = false;
    }

    private onMouseMove(event: MouseEvent) {
        if (this.lassostarted) {
            if (event.pageX < this.mousedownpoint.x) {
                this.renderer.setStyle(this.lassoRef.nativeElement, 'left', event.pageX - this.contentRect.left + 'px');
            }
            if (event.pageY < this.mousedownpoint.y) {
                this.renderer.setStyle(this.lassoRef.nativeElement, 'top', event.pageY - this.contentRect.top + 'px');
            }
            let currentWidth = event.pageX - this.mousedownpoint.x;
            let currentHeight = event.pageY - this.mousedownpoint.y;
            this.renderer.setStyle(this.lassoRef.nativeElement, 'width', Math.abs(currentWidth) + 'px');
            this.renderer.setStyle(this.lassoRef.nativeElement, 'height', Math.abs(currentHeight) + 'px');
        }
    }

    private rectangleContainsPoint(p1: Point, p2: Point, toCheck: Point): boolean {
        if (p1.x > p2.x) {
            let temp = p1.x;
            p1.x = p2.x;
            p2.x = temp;
        }
        if (p1.y > p2.y) {
            let temp = p1.y;
            p1.y = p2.y;
            p2.y = temp;
        }
        if (p1.x <= toCheck.x && p2.x >= toCheck.x && p1.y <= toCheck.y && p2.y >= toCheck.y) {
            return true;
        }
        return false;
    }
}

class SelectionNode {
    style: any;
}

class Point {
    x: number;
    y: number;
}
