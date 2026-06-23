import React, { useMemo } from "react";
import { Marker } from "@react-google-maps/api";
import type { AiRouteStop } from "../../../../types/aiRoute";
import {
  createStopMarkerSvg,
  createReturnToOfficeMarkerSvg,
  createPickupMarkerSvg,
  isReturnToOfficeStop,
  isPickupStop,
} from "../utils/routeMapUtils";

export interface RouteStopMarkerProps {
  stop: AiRouteStop;
  label: string;
  color: string;
  highlighted: boolean;
  selected: boolean;
  onClick: () => void;
}

const RouteStopMarker: React.FC<RouteStopMarkerProps> = ({
  stop,
  label,
  color,
  highlighted,
  selected,
  onClick,
}) => {
  const position = useMemo(
    () => ({ lat: stop.latitude as number, lng: stop.longitude as number }),
    [stop.latitude, stop.longitude]
  );

  const icon = useMemo(() => {
    const isReturn = isReturnToOfficeStop(stop);
    const isPickup = isPickupStop(stop);
    const size = highlighted || selected ? 40 : 34;
    let url: string;

    if (isReturn) {
      url = createReturnToOfficeMarkerSvg("VỀ BC");
    } else if (isPickup) {
      url = createPickupMarkerSvg(label);
    } else {
      url = createStopMarkerSvg(color, label, highlighted || selected, stop.stopType);
    }

    return {
      url,
      scaledSize: new google.maps.Size(size, size),
      anchor: new google.maps.Point(size / 2, size / 2),
    };
  }, [color, label, highlighted, selected, stop.stopType]);

  return (
    <Marker
      position={position}
      icon={icon}
      onClick={onClick}
      zIndex={highlighted || selected ? 900 : 400}
      animation={selected ? google.maps.Animation.BOUNCE : undefined}
    />
  );
};

export default RouteStopMarker;
