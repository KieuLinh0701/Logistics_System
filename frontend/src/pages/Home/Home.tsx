import React, { useEffect, useState } from 'react';
import HeaderHome from '../../components/common/HeaderHome';
import FooterHome from '../../components/common/FooterHome';
import HeroSection from './components/HeroSection';
import TrackingServices from './components/TrackingServices';
import InformationServices from './components/InformationServices';
import FeaturesSection from './components/FeaturesSection';
import ShippingServices from './components/ShippingServices';
import { useNavigate } from 'react-router-dom';
import type { ServiceType } from '../../types/serviceType';
import serviceTypeApi from '../../api/serviceTypeApi';

const HomePage: React.FC = () => {
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);

  const [serviceTypes, setServiceTypes] = useState<ServiceType[]>([]);

  const fetchServiceTypes = async () => {
    try {
      setLoading(true);
      const response = await serviceTypeApi.getActiveServiceTypes();
      if (response.success && response.data) {
        setServiceTypes(response.data);
      }
    } catch (error) {
      console.error("Error fetching Service types:", error);
    } finally {
      setLoading(false);
    }
  };

  const handleViewAllDetail = () => {
    navigate("/info/services")
  }

  useEffect(() => {
    fetchServiceTypes();
  }, []);

  return (
    <>
      <HeaderHome />

      <HeroSection />

      <ShippingServices
        services={serviceTypes}
        onViewAllDetails={handleViewAllDetail}
      />

      <TrackingServices />

      <InformationServices />

      <FeaturesSection />

      <FooterHome />
    </>
  );
};

export default HomePage;