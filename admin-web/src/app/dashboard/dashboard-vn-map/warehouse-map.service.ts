import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface DashboardWarehouse {
  _id?: string;
  name?: string;
  code?: string;
  address?: string;
  location?: { lat?: number; lng?: number };
  isActive?: boolean;
}

@Injectable({ providedIn: 'root' })
export class WarehouseMapService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = environment.apiUrl;

  fetchAllWarehouses(): Observable<DashboardWarehouse[]> {
    return this.http.get<DashboardWarehouse[]>(`${this.apiUrl}/warehouses`);
  }
}
