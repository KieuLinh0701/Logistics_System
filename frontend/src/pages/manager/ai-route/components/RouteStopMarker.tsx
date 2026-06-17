import React, { useMemo } from "react";
import { Marker } from "@react-google-maps/api";
import type { AiRouteStop } from "../../../../types/aiRoute";
import { createStopMarkerSvg } from "../utils/routeMapUtils";

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
    const url = createStopMarkerSvg(color, label, highlighted || selected);
    const size = highlighted || selected ? 40 : 34;
    return {
      url,
      scaledSize: new google.maps.Size(size, size),
      anchor: new google.maps.Point(size / 2, size / 2),
    };
  }, [color, label, highlighted, selected]);

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
