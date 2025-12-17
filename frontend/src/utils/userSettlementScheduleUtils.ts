export const USER_SETTLEMENT_SCHEDULE_WEEKDAY = [
  'MONDAY',
  'TUESDAY',
  'WEDNESDAY',
  'THURSDAY',
  'FRIDAY'
] as const;

export const translateUserSettlementScheduleWeekday = (value: string): string => {
  switch (value) {
    case 'MONDAY': return 'Thứ 2';
    case 'TUESDAY': return 'Thứ 3';
    case 'WEDNESDAY': return 'Thứ 4';
    case 'THURSDAY': return 'Thứ 5';
    case 'FRIDAY': return 'Thứ 6';
    default: return value; 
  }
};