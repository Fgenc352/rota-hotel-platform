export interface FailedEvent {
    id: string;
    event_type: string;
    trace_id: string;
    consumer: string;
    attempts: number;
    last_error: string;
}
