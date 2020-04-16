import { Component, OnInit } from '@angular/core';
import {PACKAGE_TYPE_WEB_COMPONENT, PACKAGE_TYPE_WEB_SERVICE, PACKAGE_TYPE_WEB_LAYOUT, PACKAGE_TYPE_SOLUTION, PACKAGE_TYPE_MODULE, Package, WpmService, PACKAGE_TYPE_TO_TITLE_MAP, ALL_PACKAGE_TYPES} from '../wpm.service'

export interface PackageList {
  title: string;
  type: string;
  updateCount: number;
  packages: Package[];
}

@Component({
  selector: 'app-content',
  templateUrl: './content.component.html',
  styleUrls: ['./content.component.css']
})
export class ContentComponent implements OnInit {

  packageLists: PackageList[] = []

  constructor(public wpmService: WpmService) { }

  ngOnInit() {
    this.wpmService.getPackages().subscribe(p => {
      const packageListIdx = this.getPackageListIdx(p.packageType);
      if(p.packages.length == 0) {
        if(packageListIdx != -1) this.packageLists.splice(packageListIdx, 1);
      }
      else {
        if(packageListIdx == -1) {
          let packageList =  {
            title: PACKAGE_TYPE_TO_TITLE_MAP[p.packageType],
            type: p.packageType,
            updateCount: this.getUpgradeCount(p.packages),
            packages: p.packages
          }
          this.packageLists.splice(this.getPackageListInsertIdx(p.packageType), 0, packageList);
        }
        else {
          this.packageLists[packageListIdx].packages = p.packages;
          this.packageLists[packageListIdx].updateCount = this.getUpgradeCount(p.packages);
        }
      }
    })
  }

  getPackageTabLabel(packageList: PackageList): string {
    return packageList.updateCount > 0 ? packageList.title + " (" + packageList.updateCount + ")" : packageList.title;
  }

  getUpgradeCount(packages: Package[]): number {
    let count = 0;
    try {
      for (let i = 0; i < packages.length; i++) {
          let pckg = packages[i];
          if (pckg.installed && pckg.installed < pckg.releases[0].version) {
            count++;
          }
      }
    } catch (e) {}
    return count;
  }

  getPackageListIdx(type: string): number {
    for (let i = 0; i < this.packageLists.length; i++) {
      if(this.packageLists[i].type == type) {
        return i;
      }
    }
    return -1;
  }

  getPackageListInsertIdx(type: string): number {
    const typeIdx = ALL_PACKAGE_TYPES.indexOf(type);
    let insertIdx = 0;
    for (let i = 0; i < this.packageLists.length; i++) {
      if(ALL_PACKAGE_TYPES.indexOf(this.packageLists[i].type) < typeIdx) {
        insertIdx = i + 1;
      }
    }
    return insertIdx;
  }
}
