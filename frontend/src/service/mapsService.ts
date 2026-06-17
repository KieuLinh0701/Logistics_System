const BASE = `${import.meta.env.VITE_API_BASE}/public`;

export interface Prediction {
    place_id: string;
    description: string;
}

export interface AutocompleteResponse {
    predictions: Prediction[];
    status: string;
}

export interface GeocodeResult {
    place_id: string;
    formatted_address: string;
    geometry: {
        location: { lat: number; lng: number };
    };
}

export interface GeocodeResponse {
    results: GeocodeResult[];
    status: string;
}

export interface DistanceElement {
    status: string;
    distance: { text: string; value: number };
    duration: { text: string; value: number };
}

export interface DistanceMatrixResponse {
    rows: { elements: DistanceElement[] }[];
    status: string;
}

export interface DirectionsResponse {
    routes: google.maps.DirectionsRoute[];
    status: string;
}

export const geocodeAddress = async (address: string): Promise<GeocodeResponse> => {
    const res = await fetch(`${BASE}/maps/geocode?address=${encodeURIComponent(address)}`);
    return res.json();
};

export const getDirections = async (
    origin: string,
    destination: string
): Promise<DirectionsResponse> => {
    const res = await fetch(
        `${BASE}/maps/directions?origin=${encodeURIComponent(origin)}&destination=${encodeURIComponent(destination)}`
    );
    return res.json();
};

export const getDistanceMatrix = async (
    origins: string,
    destinations: string
): Promise<DistanceMatrixResponse> => {
    const res = await fetch(
        `${BASE}/maps/distance-matrix?origins=${encodeURIComponent(origins)}&destinations=${encodeURIComponent(destinations)}`
    );
    return res.json();
};

export const getPlaceDetails = async (placeId: string) => {
    const res = await fetch(`${BASE}/maps/place-details?placeId=${encodeURIComponent(placeId)}`);
    return res.json();
};