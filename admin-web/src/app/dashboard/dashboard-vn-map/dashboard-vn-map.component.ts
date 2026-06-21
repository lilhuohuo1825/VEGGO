import {
  AfterViewInit,
  ChangeDetectorRef,
  Component,
  DestroyRef,
  ElementRef,
  Input,
  NgZone,
  OnChanges,
  OnDestroy,
  SimpleChanges,
  ViewChild,
  inject
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import * as L from 'leaflet';
import {
  aggregateOrdersByProvince,
  type ProvinceCentroid,
  type ProvinceOrderAgg
} from './vn-map-utils';
import { DashboardWarehouse, WarehouseMapService } from './warehouse-map.service';

export type MapDashboardMode = 'warehouses' | 'orders';

@Component({
  selector: 'app-dashboard-vn-map',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './dashboard-vn-map.component.html',
  styleUrl: './dashboard-vn-map.component.css'
})
export class DashboardVnMapComponent implements AfterViewInit, OnChanges, OnDestroy {
  @Input() orders: any[] = [];

  @ViewChild('mapHost') mapHost!: ElementRef<HTMLDivElement>;

  private readonly http = inject(HttpClient);
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly destroyRef = inject(DestroyRef);
  private readonly warehouseService = inject(WarehouseMapService);
  private readonly sanitizer = inject(DomSanitizer);
  private readonly ngZone = inject(NgZone);

  mapMode: MapDashboardMode = 'warehouses';
  provinces: ProvinceCentroid[] = [];
  selectedProvince: ProvinceCentroid | null = null;
  selectedWarehouse: DashboardWarehouse | null = null;
  selectedOrder: any | null = null;

  warehouses: DashboardWarehouse[] = [];
  loading = true;
  loadError: string | null = null;
  mapApiError: string | null = null;

  orderByProvince = new Map<string, ProvinceOrderAgg>();

  private map: L.Map | null = null;
  private markerLayer: L.LayerGroup | null = null;

  ngAfterViewInit(): void {
    this.http
      .get<ProvinceCentroid[]>('/geo/vn-province-centroids.json')
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (list) => {
          this.provinces = list;
          this.orderByProvince = aggregateOrdersByProvince(this.orders, list);
          this.loadWarehouses();
        },
        error: () => {
          this.loading = false;
          this.loadError = 'Không tải được dữ liệu địa giới tỉnh thành.';
          this.cdr.markForCheck();
        }
      });
  }

  private loadWarehouses(): void {
    this.warehouseService.fetchAllWarehouses().subscribe({
      next: (data) => {
        this.warehouses = data;
        this.loading = false;
        this.cdr.markForCheck();
        setTimeout(() => this.initMap(), 0);
      },
      error: () => {
        this.loading = false;
        this.loadError = 'Không lấy được danh sách kho hàng từ Backend.';
        this.cdr.markForCheck();
      }
    });
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['orders'] && this.provinces.length > 0 && this.map) {
      this.orderByProvince = aggregateOrdersByProvince(this.orders, this.provinces);
      this.refreshMarkers();
    }
  }

  ngOnDestroy(): void {
    this.teardownMap();
  }

  setMode(mode: MapDashboardMode): void {
    if (this.mapMode === mode) return;
    this.mapMode = mode;
    this.selectedProvince = null;
    this.selectedWarehouse = null;
    this.selectedOrder = null;
    this.refreshMarkers();
    this.cdr.markForCheck();
  }

  selectProvince(p: ProvinceCentroid): void {
    this.selectedProvince = p;
    this.selectedWarehouse = null;
    this.refreshMarkers();
    this.cdr.markForCheck();
  }

  selectWarehouse(wh: DashboardWarehouse): void {
    this.selectedWarehouse = wh;
    if (wh.location?.lat != null && wh.location?.lng != null && this.map) {
      this.map.setView([wh.location.lat, wh.location.lng], 12);
    }
    this.refreshMarkers();
    this.cdr.markForCheck();
  }

  clearProvinceSelection(): void {
    this.selectedProvince = null;
    this.selectedOrder = null;
    this.refreshMarkers();
    this.cdr.markForCheck();
  }

  clearWarehouseSelection(): void {
    this.selectedWarehouse = null;
    if (this.map) {
      this.map.setView([16.2, 106.8], 6);
    }
    this.refreshMarkers();
    this.cdr.markForCheck();
  }

  countForProvince(p: ProvinceCentroid): number {
    return this.orderByProvince.get(p.name)?.total ?? 0;
  }

  orderDetailForSelected(): ProvinceOrderAgg | null {
    if (!this.selectedProvince) return null;
    return (
      this.orderByProvince.get(this.selectedProvince.name) ?? {
        total: 0,
        homeDelivery: 0,
        pharmacyPickup: 0,
        samples: []
      }
    );
  }

  formatMoney(v: unknown): string {
    const n = Number(v);
    if (!Number.isFinite(n)) return '—';
    return `${n.toLocaleString('vi-VN')}đ`;
  }

  orderLabel(o: any): string {
    return String(o?.OrderID || o?.order_id || o?._id || '').slice(0, 20) || '—';
  }

  openOrderDetail(order: any): void {
    this.selectedOrder = order || null;
    this.cdr.markForCheck();
  }

  closeOrderDetail(): void {
    this.selectedOrder = null;
    this.cdr.markForCheck();
  }

  orderCustomerName(o: any): string {
    return o?.shippingInfo?.fullName || o?.fullName || 'Khách hàng';
  }

  orderPhone(o: any): string {
    return o?.shippingInfo?.phone || o?.phone || '';
  }

  orderAddress(o: any): string {
    const info = o?.shippingInfo;
    if (!info) return '—';
    if (info.address) {
      const a = info.address;
      return [a.detail, a.ward, a.district, a.city].filter(Boolean).join(', ');
    }
    return info.fullName || '—';
  }

  getMapUrl(wh: DashboardWarehouse): SafeResourceUrl | null {
    if (!wh.location?.lat || !wh.location?.lng) return null;
    const url = `https://maps.google.com/maps?q=${wh.location.lat},${wh.location.lng}&hl=vi&z=17&output=embed`;
    return this.sanitizer.bypassSecurityTrustResourceUrl(url);
  }

  private initMap(): void {
    if (!this.mapHost?.nativeElement || this.map) return;

    const el = this.mapHost.nativeElement;
    
    // Restrict bounds strictly to Vietnam region so it doesn't scale/pan out of boundaries
    const southWest = L.latLng(4.5, 99.0);
    const northEast = L.latLng(24.5, 114.5);
    const bounds = L.latLngBounds(southWest, northEast);

    this.map = L.map(el, {
      zoomControl: true,
      minZoom: 5,
      maxZoom: 18,
      maxBounds: bounds,
      maxBoundsViscosity: 1.0
    }).setView([16.0, 106.5], 6);

    L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
      attribution:
        '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
    }).addTo(this.map);

    this.markerLayer = L.layerGroup().addTo(this.map);

    // Call fitBounds inside a small timeout to ensure proper sizing after DOM layout is stable
    setTimeout(() => {
      if (this.map) {
        this.map.invalidateSize();
        this.map.fitBounds(
          [
            [8.2, 102.0],
            [23.9, 110.2]
          ],
          { padding: [10, 10] }
        );
      }
    }, 100);

    this.map.on('resize', () => {
      this.map?.invalidateSize();
    });
    this.mapApiError = null;
    this.refreshMarkers();
    this.cdr.markForCheck();
  }

  private teardownMap(): void {
    this.markerLayer?.clearLayers();
    this.markerLayer = null;
    if (this.map) {
      this.map.remove();
      this.map = null;
    }
  }

  private refreshMarkers(): void {
    if (!this.map || !this.markerLayer) return;
    this.markerLayer.clearLayers();

    if (this.mapMode === 'orders') {
      this.plotProvinceCircles();
    } else {
      this.plotWarehouseMarkers();
    }
  }

  private plotProvinceCircles(): void {
    if (!this.map || !this.markerLayer) return;
    const maxVal = Math.max(1, ...this.provinces.map((p) => this.countForProvince(p)));

    for (const p of this.provinces) {
      const count = this.countForProvince(p);
      if (count <= 0) continue;
      const t = maxVal > 0 ? Math.log1p(count) / Math.log1p(maxVal) : 0;
      const radius = Math.min(15, 6 + t * 9);
      const fillOpacity = 0.3 + t * 0.2;
      const fill = '#F2CD38'; // Brand Yellow
      const stroke = 'rgba(242, 205, 56, 0.5)';

      const cm = L.circleMarker([p.lat, p.lng], {
        radius,
        color: stroke,
        weight: 1.5,
        fillColor: fill,
        fillOpacity,
        opacity: 0.95
      });
      cm.bindTooltip(
        `${p.name} · ${count} đơn hàng`,
        { direction: 'top', sticky: true, opacity: 0.95, className: 'vn-map-tooltip' }
      );
      cm.on('click', () => {
        this.ngZone.run(() => this.selectProvince(p));
      });
      cm.addTo(this.markerLayer);
    }
  }

  private plotWarehouseMarkers(): void {
    if (!this.map || !this.markerLayer) return;

    for (const wh of this.warehouses) {
      if (!wh.location?.lat || !wh.location?.lng) continue;
      const lat = wh.location.lat;
      const lng = wh.location.lng;

      const sel = this.selectedWarehouse && this.selectedWarehouse.code === wh.code;

      const cm = L.circleMarker([lat, lng], {
        radius: sel ? 12 : 8,
        color: sel ? '#1F5B0C' : '#3CB018',
        weight: sel ? 3 : 2,
        fillColor: sel ? '#62FF2B' : '#3CB018',
        fillOpacity: sel ? 0.9 : 0.7,
        opacity: 1
      });
      cm.bindTooltip(
        `Kho: ${wh.name} (${wh.code}) — ${wh.address}`,
        { direction: 'top', sticky: true, opacity: 0.98, className: 'vn-map-tooltip' }
      );
      cm.on('click', () => {
        this.ngZone.run(() => this.selectWarehouse(wh));
      });
      cm.addTo(this.markerLayer);
    }
  }
}
