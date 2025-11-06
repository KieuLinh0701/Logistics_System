import React from 'react';
import HeaderHome from '../../components/common/HeaderHome';
import FooterHome from '../../components/common/FooterHome';
import HeroSection from './components/HeroSection';
import TrackingServices from './components/TrackingServices';
import InformationServices from './components/InformationServices';
import FeaturesSection from './components/FeaturesSection';
import ShippingServices from './components/ShippingServices';

const HomePage: React.FC = () => {
  return (
    <>
      <HeaderHome />

      <HeroSection />

      <ShippingServices />

      <TrackingServices />

      <InformationServices />

      <FeaturesSection />

      <FooterHome />
    </>
  );
};

export default HomePage;