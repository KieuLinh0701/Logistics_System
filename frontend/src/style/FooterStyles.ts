import type { CSSProperties } from 'react';

export const COLORS = {
  primary: '#1C3D90',
  text: '#7A7A7A',
  divider: '#000',
};

export const styles: Record<string, CSSProperties> = {
  footer: {
    backgroundColor: '#E0E0E0',
    color: '#f0f0f0',
    padding: '60px 90px',
  },
  title: {
    color: COLORS.primary,
    margin: 0,
    fontSize: '40px',
  },
  sectionTitle: {
    color: '#000',
    margin: 0,
    marginBottom: '12px',
    fontSize: '28px',
  },
  text: {
    color: COLORS.text,
    fontSize: '15px',
    lineHeight: 1.8,
  },
  subText: {
    color: '#8c8c8c',
    fontSize: '13px',
  },
  linkList: {
    display: 'flex',
    flexDirection: 'column',
    gap: '12px',
  },
  link: {
    color: COLORS.text,
    fontSize: '15px',
    transition: 'color 0.3s ease',
  },
  icon: {
    marginRight: 8,
  },
  divider: {
    borderColor: COLORS.divider,
    margin: '20px 0 40px 0',
  },
};

export const aboutLinks = [
  { key: 'about', path: '/info/company', text: 'Về chúng tôi' },
  { key: 'contact', path: '/info/contact', text: 'Liên hệ' },
  { key: 'consulting', path: '/consulting', text: 'Tư vấn' },
];