export const translateOfficeType = (value: string): string => {
  switch (value) {
    case 'HEAD_OFFICE': return 'Trụ sở chính';       
    case 'POST_OFFICE': return 'Bưu cục';          
    default: return value;
  }
};