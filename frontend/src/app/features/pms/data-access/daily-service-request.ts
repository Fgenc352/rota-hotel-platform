import type { DailyTask } from '../models/daily-operations.model';
/** Keep read-model IDs and audit fields out of the strict write contract. */
export function dailyServiceRequest(task: DailyTask) {
    return {
        date: task.date,
        status: task.status,
        time: task.time || null,
        pickup: task.pickup,
        destination: task.destination,
        plate: task.plate,
        notes: task.notes,
        passengers: task.passengers
    };
}
