import type { Address } from "./address";
import type { User } from "./user";
import type { Order } from "./orderHistory";

export interface Incident {
  id: number;
  code: string;
  order: Order;
  shipper: User;
  incidentType: string;
  title: string;
  description: string;
  address: Address;
  priority: string;
  images: string[];
  status: string;
  resolution: string;
  handler: User;
  handledAt: string;
  createdAt: string;
  updatedAt: string;
}

export interface ManagerIncidentUpdateRequest {
  status: string;
  resolution: string;
}