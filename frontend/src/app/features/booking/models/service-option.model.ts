export type ServiceUnit = 'PER_STAY' | 'PER_NIGHT' | 'PER_PERSON_PER_NIGHT';
export interface ServiceOption {
    code: string;
    name: string;
    description: string;
    price: number;
    unit: ServiceUnit;
    included: boolean;
}
export interface ServiceLine {
    code: string;
    name: string;
    unit: ServiceUnit;
    unitPrice: number;
    quantity: number;
    total: number;
    included: boolean;
}
