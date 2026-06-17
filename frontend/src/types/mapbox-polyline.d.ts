declare module "@mapbox/polyline" {
  type LatLngTuple = [number, number];

  export function decode(
    str: string,
    precision?: number
  ): LatLngTuple[];

  export function encode(
    coordinates: LatLngTuple[],
    precision?: number
  ): string;

  const polyline: {
    decode: typeof decode;
    encode: typeof encode;
  };

  export default polyline;
}
