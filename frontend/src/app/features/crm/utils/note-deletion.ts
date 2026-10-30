import type { GuestNote } from '../models/guest-note.model';

export type NoteDeletionState = 'available' | 'expired' | 'not-author';

const deletionWindowMs = 24 * 60 * 60 * 1000;

export function noteDeletionState(note: GuestNote, username: string, now: number): NoteDeletionState {
    if (note.author !== username) {
        return 'not-author';
    }
    const createdAt = Date.parse(note.created_at);
    if (!Number.isFinite(createdAt) || now < createdAt || now - createdAt >= deletionWindowMs) {
        return 'expired';
    }
    return 'available';
}
