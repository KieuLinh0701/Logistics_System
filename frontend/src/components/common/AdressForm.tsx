import React, {useCallback, useEffect, useRef, useState} from "react";
import {Form, Input, message, Select} from "antd";
import type {City, Ward} from "../../types/location";
import locationApi from "../../api/locationApi";
import type {Prediction} from "../../service/mapsService";
import {geocodeAddress, getPlaceDetails} from "../../service/mapsService";

const { Option } = Select;

interface AddressFormProps {
    form: any;
    prefix: string;
    initialCity?: number;
    initialCityName?: string;
    initialWard?: number;
    initialWardName?: string;
    initialDetail?: string;
    initialLatitude?: number;
    initialLongitude?: number;
    disableCity?: boolean;
    disableWard?: boolean;
    disableDetailAddress?: boolean;
    onManualChange?: () => void;
}

const AddressForm: React.FC<AddressFormProps> = ({
                                                     form,
                                                     prefix,
                                                     initialCity,
                                                     initialCityName,
                                                     initialWard,
                                                     initialWardName,
                                                     initialDetail,
                                                     initialLatitude,
                                                     initialLongitude,
                                                     disableCity,
                                                     disableWard,
                                                     disableDetailAddress,
                                                     onManualChange,
                                                 }) => {
    const [cities, setCities] = useState<City[]>([]);
    const [wards, setWards] = useState<Ward[]>([]);
    const [suggestions, setSuggestions] = useState<Prediction[]>([]);
    const [addressInput, setAddressInput] = useState("");
    const [wardsLoading, setWardsLoading] = useState(false);

    const autocompleteServiceRef = useRef<google.maps.places.AutocompleteService | null>(null);
    const debounceRef = useRef<ReturnType<typeof setTimeout> | null>(null);
    const activeCityCodeRef = useRef<number | undefined>(initialCity);
    const initDoneRef = useRef(false);

    const selectedCity = Form.useWatch([prefix, "cityCode"], form);

    // ── extra fields lưu trong ref (sync) + form store (để parent đọc) ────────
    const extraRef = useRef({
        cityName:  initialCityName,
        wardName:  initialWardName,
        latitude:  initialLatitude,
        longitude: initialLongitude,
    });

    const setExtra = useCallback(
        (patch: Partial<typeof extraRef.current>) => {
            extraRef.current = { ...extraRef.current, ...patch };

            const allValues = form.getFieldsValue(true);

            form.setFieldsValue({
                ...allValues,
                [prefix]: {
                    ...(allValues[prefix] || {}),
                    ...extraRef.current,
                },
            });
        },
        [form, prefix]
    );

    useEffect(() => {
        const init = () => {
            if (window.google?.maps?.places) {
                autocompleteServiceRef.current =
                    new window.google.maps.places.AutocompleteService();
            }
        };
        if (window.google?.maps?.places) {
            init();
        } else {
            const existing = document.querySelector(`script[src*="maps.googleapis.com"]`);
            if (existing) { existing.addEventListener("load", init); return; }
            const script = document.createElement("script");
            script.src = `https://maps.googleapis.com/maps/api/js?key=${
                import.meta.env.VITE_GOOGLE_MAPS_KEY
            }&libraries=places`;
            script.async = true;
            script.onload = init;
            document.head.appendChild(script);
        }
    }, []);

    useEffect(() => {
        locationApi.getCities().then(setCities).catch(console.error);
    }, []);


    useEffect(() => {
        if (!initialCity) {
            initDoneRef.current = true;
            return;
        }

        locationApi.getWardsByCity(Number(initialCity)).then((wardList) => {
            setWards(wardList);

            form.setFieldsValue({
                [prefix]: {
                    ...form.getFieldValue(prefix),
                    cityCode:  initialCity,
                    wardCode:  initialWard  ?? null,
                    detail:    initialDetail ?? "",
                    cityName:  initialCityName,
                    wardName:  initialWardName,
                    latitude:  initialLatitude,
                    longitude: initialLongitude,
                },
            });

            extraRef.current = {
                cityName:  initialCityName,
                wardName:  initialWardName,
                latitude:  initialLatitude,
                longitude: initialLongitude,
            };

            initDoneRef.current = true;

            const isInvalidCoords =
                !initialLatitude || !initialLongitude ||
                initialLatitude === 0 || initialLongitude === 0;

            if (isInvalidCoords && initialCityName && initialWardName && initialDetail?.trim()) {
                const attempts = [
                    [initialDetail, initialWardName, initialCityName, "Việt Nam"].join(", "),
                    [initialWardName, initialCityName, "Việt Nam"].join(", "),
                ];
                (async () => {
                    for (const address of attempts) {
                        try {
                            const data = await geocodeAddress(address);
                            if (data?.results?.[0]?.geometry?.location) {
                                const { lat, lng } = data.results[0].geometry.location;
                                const isValidVN = lat >= 8.0 && lat <= 23.5 && lng >= 102.0 && lng <= 110.0;
                                if (isValidVN) {
                                    extraRef.current = { ...extraRef.current, latitude: lat, longitude: lng };
                                    form.setFieldsValue({
                                        [prefix]: {
                                            ...form.getFieldValue(prefix),
                                            latitude: lat,
                                            longitude: lng,
                                        },
                                    });
                                    return;
                                }
                            }
                        } catch (err) {
                            console.error("Geocode thất bại:", err);
                        }
                    }
                    message.warning('Không xác định được tọa độ, vui lòng nhập chi tiết địa chỉ rõ hơn!');
                })();
            }
        });
    }, []); // eslint-disable-line react-hooks/exhaustive-deps

    useEffect(() => {
        if (!initDoneRef.current) return;
        if (!selectedCity) return;
        if (selectedCity === activeCityCodeRef.current) return;

        activeCityCodeRef.current = selectedCity;
        setWardsLoading(true);
        locationApi.getWardsByCity(Number(selectedCity)).then((wardList) => {
            setWards(wardList);
            setWardsLoading(false);
        });
    }, [selectedCity]); // eslint-disable-line react-hooks/exhaustive-deps

    // ─── Helpers ──────────────────────────────────────────────────────────────
    const normalizeStr = useCallback(
        (str: string) =>
            str
                .normalize("NFD")
                .replace(/[\u0300-\u036f]/g, "")
                .toLowerCase()
                .trim()
                .replace(/^(thanh pho |tinh |thi xa |quan |huyen |phuong |xa |thi tran |tp\.? ?)/, "")
                .trim(),
        []
    );

    const matchWard = useCallback(
        (wardList: Ward[], name: string): Ward | undefined => {
            const q = normalizeStr(name);
            if (!q) return undefined;
            const exact    = wardList.find((w) => normalizeStr(w.name) === q);
            if (exact) return exact;
            const contains = wardList.find((w) => q.includes(normalizeStr(w.name)) && normalizeStr(w.name).length > 3);
            if (contains) return contains;
            const siblings = wardList.filter((w) => normalizeStr(w.name).startsWith(q));
            if (siblings.length === 1) return siblings[0];
            return undefined;
        },
        [normalizeStr]
    );

    const handleGeocode = useCallback(
        async (cityName: string, wardName: string, detail: string) => {
            if (!cityName || !wardName || !detail?.trim()) {
                setExtra({ latitude: 0, longitude: 0 });
                return;
            }
            const attempts = [
                [detail, wardName, cityName, "Việt Nam"].join(", "),
                [wardName, cityName, "Việt Nam"].join(", "),
            ];
            for (const address of attempts) {
                try {
                    const data = await geocodeAddress(address);
                    if (data?.results?.[0]?.geometry?.location) {
                        const { lat, lng } = data.results[0].geometry.location;

                        const isValidVN = lat >= 8.0 && lat <= 23.5 && lng >= 102.0 && lng <= 110.0;
                        if (isValidVN) {
                            setExtra({ latitude: lat, longitude: lng });
                            return;
                        }
                    }
                } catch (err) {
                    console.error("Geocode thất bại:", err);
                }
            }
            setExtra({ latitude: 0, longitude: 0 });
            message.warning('Không xác định được tọa độ, vui lòng nhập chi tiết địa chỉ rõ hơn!');
        },
        [setExtra]
    );


    const handleAddressInput = (val: string) => {
        setAddressInput(val);
        if (debounceRef.current) clearTimeout(debounceRef.current);
        if (val.length < 3) return setSuggestions([]);
        debounceRef.current = setTimeout(() => {
            if (!autocompleteServiceRef.current) return;
            autocompleteServiceRef.current.getPlacePredictions(
                { input: val, language: "vi", componentRestrictions: { country: "vn" } },
                (predictions, status) => {
                    if (status === window.google.maps.places.PlacesServiceStatus.OK && predictions) {
                        setSuggestions(predictions.map((p) => ({ place_id: p.place_id, description: p.description })));
                    } else {
                        setSuggestions([]);
                    }
                }
            );
        }, 300);
    };

    const handleSelectSuggestion = async (prediction: Prediction) => {
        setAddressInput(prediction.description);
        setSuggestions([]);
        try {
            const data = await getPlaceDetails(prediction.place_id);
            const components: { long_name: string; short_name: string; types: string[] }[] =
                data?.result?.address_components || [];
            const formattedAddress: string = data?.result?.formatted_address || "";

            const provinceName = components.find((c) => c.types.includes("administrative_area_level_1"))?.long_name || "";
            const districtName = components.find((c) => c.types.includes("administrative_area_level_2"))?.long_name || "";
            const level3       = components.find((c) => c.types.includes("administrative_area_level_3"))?.long_name || "";
            const subLocality1 = components.find((c) => c.types.includes("sublocality_level_1"))?.long_name || "";
            const subLocality2 = components.find((c) => c.types.includes("sublocality_level_2"))?.long_name || "";
            const locality     = components.find((c) => c.types.includes("locality"))?.long_name || "";

            const wardFromFormatted = (() => {
                if (!formattedAddress) return "";
                const parts    = formattedAddress.split(",").map((p) => p.trim());
                const filtered = parts.filter(
                    (p) =>
                        !normalizeStr(p).includes(normalizeStr(provinceName)) &&
                        !normalizeStr(p).includes("viet nam") &&
                        !normalizeStr(p).includes("vietnam")
                );
                return filtered[filtered.length - 1] || "";
            })();

            const wardNameToMatch = level3 || subLocality1 || subLocality2 || wardFromFormatted || districtName || locality;
            const streetNumber    = components.find((c) => c.types.includes("street_number"))?.long_name || "";
            const route           = components.find((c) => c.types.includes("route"))?.long_name || "";
            const detail          = [streetNumber, route].filter(Boolean).join(" ");

            const normalizedProvince = normalizeStr(provinceName);
            const matchedCity = cities.find((c) => {
                const dbCity = normalizeStr(c.name);
                return normalizedProvince === dbCity || normalizedProvince.includes(dbCity) || dbCity.includes(normalizedProvince);
            });

            if (!matchedCity) {
                form.setFieldsValue({ [prefix]: { ...form.getFieldValue(prefix), detail: detail || prediction.description } });
                return;
            }

            const wardList    = await locationApi.getWardsByCity(matchedCity.code);
            setWards(wardList);

            const matchedWard = matchWard(wardList, wardNameToMatch);
            const lat         = data?.result?.geometry?.location?.lat;
            const lng         = data?.result?.geometry?.location?.lng;

            activeCityCodeRef.current = matchedCity.code;

            form.setFieldsValue({
                [prefix]: {
                    ...form.getFieldValue(prefix),
                    cityCode: matchedCity.code,
                    wardCode: matchedWard?.code ?? undefined,
                    detail:   detail || prediction.description,
                },
            });

            setExtra({
                cityName:  matchedCity.name,
                wardName:  matchedWard?.name ?? "",
                latitude:  lat,
                longitude: lng,
            });
        } catch (err) {
            console.error("Lỗi parse địa chỉ:", err);
            form.setFieldsValue({ [prefix]: { ...form.getFieldValue(prefix), detail: prediction.description } });
        }
    };

    const handleCityChange = (newCity: number) => {
        activeCityCodeRef.current = newCity;
        const cityName = cities.find((c) => c.code === newCity)?.name ?? "";

        form.setFieldsValue({
            [prefix]: {
                ...form.getFieldValue(prefix),
                cityCode: newCity,
                wardCode: null,
                wardName: "",
            },
        });

        setExtra({ cityName, wardName: "", latitude: 0, longitude: 0 });
        onManualChange?.();
    };

    const handleWardChange = (value: number) => {
        const wardName = wards.find((w) => w.code === value)?.name ?? "";
        const cityName = extraRef.current.cityName
            || cities.find((c) => c.code === form.getFieldValue([prefix, "cityCode"]))?.name
            || "";
        const detail   = form.getFieldValue([prefix, "detail"]) ?? "";

        setExtra({ wardName, latitude: 0, longitude: 0 });
        handleGeocode(cityName, wardName, detail);
        onManualChange?.();
    };

    const handleDetailChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        onManualChange?.();
        const value = e.target.value;
        if (debounceRef.current) clearTimeout(debounceRef.current);
        debounceRef.current = setTimeout(() => {
            const cityName = extraRef.current.cityName ?? "";
            const wardName = extraRef.current.wardName ?? "";
            handleGeocode(cityName, wardName, value);
        }, 800);
    };

    // ─── Render ───────────────────────────────────────────────────────────────
    return (
        <>
            {!disableCity && (
                <Form.Item label={<span className="modal-lable">Tìm địa chỉ nhanh</span>}>
                    <div style={{ position: "relative" }}>
                        <Input
                            className="modal-custom-input"
                            placeholder="Gõ địa chỉ để tự động điền..."
                            value={addressInput}
                            onChange={(e) => handleAddressInput(e.target.value)}
                            allowClear
                            onClear={() => { setAddressInput(""); setSuggestions([]); }}
                        />
                        {suggestions.length > 0 && (
                            <ul className="dropdown-list">
                                {suggestions.map((s) => (
                                    <li key={s.place_id} className="dropdown-item"
                                        onClick={() => handleSelectSuggestion(s)}>
                                        {s.description}
                                    </li>
                                ))}
                            </ul>
                        )}
                    </div>
                </Form.Item>
            )}

            <Form.Item
                name={[prefix, "cityCode"]}
                label={<span className="modal-lable">Tỉnh / Thành phố</span>}
                rules={[{ required: true, message: "Vui lòng chọn tỉnh / thành phố!" }]}
            >
                <Select
                    className="modal-custom-select"
                    showSearch placeholder="Chọn tỉnh/thành phố"
                    optionFilterProp="label" disabled={disableCity}
                    onChange={handleCityChange}
                    filterOption={(input, option) =>
                        (option?.label as string).toLowerCase().includes(input.toLowerCase())
                    }
                >
                    {cities.map((c) => (
                        <Option key={c.code} value={c.code} label={c.name}>{c.name}</Option>
                    ))}
                </Select>
            </Form.Item>

            <Form.Item
                name={[prefix, "wardCode"]}
                label={<span className="modal-lable">Phường / Xã</span>}
                rules={[{ required: true, message: "Vui lòng chọn phường / xã!" }]}
            >
                <Select
                    loading={wardsLoading}
                    className="modal-custom-select"
                    showSearch placeholder="Chọn phường/xã"
                    optionFilterProp="label"
                    disabled={!selectedCity || disableWard}
                    onChange={handleWardChange}
                    filterOption={(input, option) =>
                        (option?.label as string).toLowerCase().includes(input.toLowerCase())
                    }
                >
                    {wards.map((w) => (
                        <Option key={w.code} value={w.code} label={w.name}>{w.name}</Option>
                    ))}
                </Select>
            </Form.Item>

            <Form.Item
                name={[prefix, "detail"]}
                label={<span className="modal-lable">Chi tiết</span>}
                rules={[{ required: true, message: "Vui lòng nhập số nhà, tên đường!" }]}
            >
                <Input
                    className="modal-custom-input"
                    placeholder="Số nhà, tên đường..."
                    disabled={disableDetailAddress}
                    onChange={handleDetailChange}
                />
            </Form.Item>

            {/* Field ẩn — register vào form store để parent đọc được qua getFieldsValue */}
            <Form.Item name={[prefix, "cityName"]} hidden><input type="hidden" /></Form.Item>
            <Form.Item name={[prefix, "wardName"]} hidden><input type="hidden" /></Form.Item>
            <Form.Item name={[prefix, "latitude"]} hidden><input type="hidden" /></Form.Item>
            <Form.Item name={[prefix, "longitude"]} hidden><input type="hidden" /></Form.Item>
        </>
    );
};

export default AddressForm;