8;
import type { FunnelStage } from './funnel-stage.model';
import type { DeliverySummary } from './delivery-summary.model';
export interface Analytics {
    funnel: FunnelStage[];
    reservations: number;
    deliveries: DeliverySummary[];
}
