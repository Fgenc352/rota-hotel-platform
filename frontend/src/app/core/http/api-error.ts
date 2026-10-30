export class ApiError extends Error {
    constructor(message: string, readonly code: string, readonly traceId: string = '', readonly status: number = 0) {
        super(message + (traceId ? ' [' + traceId + ']' : ''));
    }
}
