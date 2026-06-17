import React, {useEffect, useRef, useState} from "react";
import {Form, Input, Select} from "antd";
import type {City, Ward} from "../../types/location";
import locationApi from "../../api/locationApi";
import {geocodeAddress, getPlaceDetails} from "../../service/mapsService";
import type {Prediction} from "../../service/mapsService";

const {Option} = Select;

interface AddressFormProps {
    form: any;
    prefix: string;
    initialCity?: number;
    initialWard?: number;
    initialDetail?: string;
    disableCity?: boolean;
    disableWard?: boolean;
    disableDetailAddress?: boolean;
    onManualChange?: () => void;
}

const AddressForm: React.FC<AddressFormProps> = ({
                                                     form,
                                                     prefix,
                                                     initialCity,
                                                     initialWard,
                                                     initialDetail,
                                                     disableCity,
                                                     disableWard,
                                                     disableDetailAddress,
                                                     onManualChange
                                                 }) => {
    const [cities, setCities] = useState<City[]>([]);
    const [wards, setWards] = useState<Ward[]>([]);
    const [suggestions, setSuggestions] = useState<Prediction[]>([]);
    const [addressInput, setAddressInput] = useState("");
    const [isFirstLoad, setIsFirstLoad] = useState(true);
    const [wardsLoading, setWardsLoading] = useState(false);

    const autocompleteServiceRef = useRef<google.maps.places.AutocompleteService | null>(null);
    const debounceRef = useRef<ReturnType<typeof setTimeout> | null>(null);
    const selectedCity = Form.useWatch([prefix, "cityCode"], form);
    const [currentCityCode, setCurrentCityCode] = useState<number | undefined>(initialCity);
    const skipNextWardSet = useRef(false);

    useEffect(() => {
        const initAutocomplete = () => {
            if (window.google?.maps?.places) {
                autocompleteServiceRef.current =
                    new window.google.maps.places.AutocompleteService();
            }
        };

        if (window.google?.maps?.places) {
            initAutocomplete();
        } else {
            const existing = document.querySelector(`script[src*="maps.googleapis.com"]`);
            if (existing) {
                existing.addEventListener("load", initAutocomplete);
                return;
            }
            const script = document.createElement("script");
            script.src = `https://maps.googleapis.com/maps/api/js?key=${import.meta.env.VITE_GOOGLE_MAPS_KEY}&libraries=places`;
            script.async = true;
            script.onload = initAutocomplete;
            document.head.appendChild(script);
        }
    }, []);

    useEffect(() => {
        locationApi.getCities().then(setCities).catch(console.error);
    }, []);

    useEffect(() => {
        if (initialDetail) {
            form.setFieldsValue({
                [prefix]: {...form.getFieldValue(prefix), detail: initialDetail},
            });
        }
    }, [initialDetail, form, prefix]);

    useEffect(() => {
        const cityCode = selectedCity || initialCity;
        console.log("=== useEffect fired ===", { cityCode, selectedCity, initialCity, skipNextWardSet: skipNextWardSet.current, isFirstLoad });
        if (!cityCode) return;

        setWardsLoading(true);
        locationApi.getWardsByCity(Number(cityCode)).then((wardList) => {
            console.log("=== wardList fetched ===", { skip: skipNextWardSet.current, isFirstLoad, initialWard, wardCount: wardList.length });
            setWards(wardList);
            setWardsLoading(false);

            if (skipNextWardSet.current) {
                skipNextWardSet.current = false;
                return;
            }

            if (initialWard && isFirstLoad) {
                console.log("=== SETTING initialWard ===", initialWard);
                form.setFieldsValue({
                    [prefix]: {...form.getFieldValue(prefix), wardCode: initialWard},
                });
                setIsFirstLoad(false);
            }
        });
    }, [selectedCity, initialCity]);

    const normalizeStr = (str: string) =>
        str
            .normalize("NFD")
            .replace(/[\u0300-\u036f]/g, "")
            .toLowerCase()
            .trim()
            .replace(/^(thanh pho |tinh |thi xa |quan |huyen |phuong |xa |thi tran |tp\.? ?)/, "")
            .trim();

    const handleAddressInput = (val: string) => {
        setAddressInput(val);
        if (debounceRef.current) clearTimeout(debounceRef.current);
        if (val.length < 3) return setSuggestions([]);

        debounceRef.current = setTimeout(() => {
            if (!autocompleteServiceRef.current) return;
            autocompleteServiceRef.current.getPlacePredictions(
                {input: val, language: "vi", componentRestrictions: {country: "vn"}},
                (predictions, status) => {
                    if (status === window.google.maps.places.PlacesServiceStatus.OK && predictions) {
                        setSuggestions(predictions.map((p) => ({
                            place_id: p.place_id,
                            description: p.description,
                        })));
                    } else {
                        setSuggestions([]);
                    }
                }
            );
        }, 300);
    };

    const matchWard = (wardList: Ward[], wardNameToMatch: string): Ward | undefined => {
        const normalizedQuery = normalizeStr(wardNameToMatch);
        if (!normalizedQuery) return undefined;

        const exact = wardList.find((w) => normalizeStr(w.name) === normalizedQuery);
        if (exact) return exact;

        const queryContainsDb = wardList.find(
            (w) => normalizedQuery.includes(normalizeStr(w.name)) && normalizeStr(w.name).length > 3
        );
        if (queryContainsDb) return queryContainsDb;

        const siblings = wardList.filter((w) => normalizeStr(w.name).startsWith(normalizedQuery));
        if (siblings.length === 1) return siblings[0];

        return undefined;
    };

    const handleGeocode = async (cityName: string, wardName: string, detail: string) => {
        if (!cityName || !wardName) return;

        if (!detail || detail.trim() === "") {
            form.setFieldsValue({
                [prefix]: {
                    ...form.getFieldValue(prefix),
                    latitude: 0,
                    longitude: 0,
                },
            });
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
                    const {lat, lng} = data.results[0].geometry.location;
                    form.setFieldsValue({
                        [prefix]: {
                            ...form.getFieldValue(prefix),
                            latitude: lat,
                            longitude: lng,
                        },
                    });
                    return;
                }
            } catch (err) {
                console.error("Geocode thất bại:", err);
            }
        }

        form.setFieldsValue({
            [prefix]: {
                ...form.getFieldValue(prefix),
                latitude: 0,
                longitude: 0,
            },
        });
    };

    const handleSelectSuggestion = async (prediction: Prediction) => {
        setAddressInput(prediction.description);
        setSuggestions([]);

        try {
            const data = await getPlaceDetails(prediction.place_id);
            const components: {long_name: string; short_name: string; types: string[]}[] =
                data?.result?.address_components || [];
            const formattedAddress: string = data?.result?.formatted_address || "";

            const provinceName =
                components.find((c) => c.types.includes("administrative_area_level_1"))?.long_name || "";
            const districtName =
                components.find((c) => c.types.includes("administrative_area_level_2"))?.long_name || "";
            const level3 =
                components.find((c) => c.types.includes("administrative_area_level_3"))?.long_name || "";
            const subLocality1 =
                components.find((c) => c.types.includes("sublocality_level_1"))?.long_name || "";
            const subLocality2 =
                components.find((c) => c.types.includes("sublocality_level_2"))?.long_name || "";
            const locality =
                components.find((c) => c.types.includes("locality"))?.long_name || "";

            const wardFromFormatted = (() => {
                if (!formattedAddress) return "";
                const parts = formattedAddress.split(",").map((p) => p.trim());
                const filtered = parts.filter(
                    (p) =>
                        !normalizeStr(p).includes(normalizeStr(provinceName)) &&
                        !normalizeStr(p).includes("viet nam") &&
                        !normalizeStr(p).includes("vietnam")
                );
                return filtered[filtered.length - 1] || "";
            })();

            const wardNameToMatch =
                level3 || subLocality1 || subLocality2 || wardFromFormatted || districtName || locality;

            const streetNumber =
                components.find((c) => c.types.includes("street_number"))?.long_name || "";
            const route =
                components.find((c) => c.types.includes("route"))?.long_name || "";
            const detail = [streetNumber, route].filter(Boolean).join(" ");

            const normalizedProvince = normalizeStr(provinceName);
            const matchedCity = cities.find((c) => {
                const dbCity = normalizeStr(c.name);
                return (
                    normalizedProvince === dbCity ||
                    normalizedProvince.includes(dbCity) ||
                    dbCity.includes(normalizedProvince)
                );
            });

            if (!matchedCity) {
                form.setFieldsValue({
                    [prefix]: {
                        ...form.getFieldValue(prefix),
                        detail: detail || prediction.description,
                    },
                });
                return;
            }

            const wardList = await locationApi.getWardsByCity(matchedCity.code);
            setWards(wardList);

            const matchedWard = matchWard(wardList, wardNameToMatch);
            const lat = data?.result?.geometry?.location?.lat;
            const lng = data?.result?.geometry?.location?.lng;

            form.setFieldsValue({
                [prefix]: {
                    ...form.getFieldValue(prefix),
                    cityCode: matchedCity.code,
                    wardCode: matchedWard?.code ?? undefined,
                    detail: detail || prediction.description,
                    latitude: lat,
                    longitude: lng,
                    cityName: matchedCity.name,
                    wardName: matchedWard?.name ?? "",
                },
            });
        } catch (err) {
            console.error("Lỗi parse địa chỉ:", err);
            form.setFieldsValue({
                [prefix]: {
                    ...form.getFieldValue(prefix),
                    detail: prediction.description,
                },
            });
        }
    };

    const handleCityChange = (newCity: number) => {
        setWards([]);
        setCurrentCityCode(newCity);
        skipNextWardSet.current = true;

        const cityName = cities.find(c => c.code === newCity)?.name ?? "";

        const current = form.getFieldValue(prefix) ?? {};
        form.setFieldsValue({
            [prefix]: {
                ...current,
                cityCode: newCity,
                cityName,
                wardCode: null,
                wardName: "",
                latitude: 0,
                longitude: 0,
            },
        });

        setIsFirstLoad(false);
        onManualChange?.();
    };

    const handleDetailChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        onManualChange?.();
        if (debounceRef.current) clearTimeout(debounceRef.current);
        debounceRef.current = setTimeout(() => {
            const current = form.getFieldValue(prefix);
            const cityName = cities.find(c => c.code === current.cityCode)?.name ?? "";
            handleGeocode(cityName, current.wardName ?? "", e.target.value);
        }, 800);
    };

    return (
        <>
            {!disableCity && (
                <Form.Item label={<span className="modal-lable">Tìm địa chỉ nhanh</span>}>
                    <div style={{position: "relative"}}>
                        <Input
                            className="modal-custom-input"
                            placeholder="Gõ địa chỉ để tự động điền..."
                            value={addressInput}
                            onChange={(e) => handleAddressInput(e.target.value)}
                            allowClear
                            onClear={() => {
                                setAddressInput("");
                                setSuggestions([]);
                            }}
                        />
                        {suggestions.length > 0 && (
                            <ul className="dropdown-list">
                                {suggestions.map((s) => (
                                    <li
                                        key={s.place_id}
                                        className="dropdown-item"
                                        onClick={() => handleSelectSuggestion(s)}
                                    >
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
                rules={[{required: true, message: "Vui lòng chọn tỉnh / thành phố!"}]}
            >
                <Select
                    className="modal-custom-select"
                    showSearch
                    placeholder="Chọn tỉnh/thành phố"
                    optionFilterProp="label"
                    disabled={disableCity}
                    onChange={(value) => handleCityChange(value)}
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
                key={`ward-${currentCityCode}`}   // ← dùng state thay vì useWatch
                name={[prefix, "wardCode"]}
                label={<span className="modal-lable">Phường / Xã</span>}
                rules={[{required: true, message: "Vui lòng chọn phường / xã!"}]}
            >
                <Select
                    loading={wardsLoading}
                    onChange={(value) => {
                        const wardName = wards.find(w => w.code === value)?.name ?? "";
                        form.setFieldsValue({
                            [prefix]: {
                                ...form.getFieldValue(prefix),
                                wardName,
                                latitude: 0,
                                longitude: 0,
                            },
                        });
                        const current = form.getFieldValue(prefix);
                        const cityName = cities.find(c => c.code === current.cityCode)?.name ?? "";
                        handleGeocode(cityName, wardName, current.detail ?? "");
                        onManualChange?.();
                    }}
                    className="modal-custom-select"
                    showSearch
                    placeholder="Chọn phường/xã"
                    optionFilterProp="label"
                    disabled={!selectedCity || disableWard}
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
                rules={[{required: true, message: "Vui lòng nhập số nhà, tên đường!"}]}
            >
                <Input
                    className="modal-custom-input"
                    placeholder="Số nhà, tên đường..."
                    disabled={disableDetailAddress}
                    onChange={handleDetailChange}
                />
            </Form.Item>

            <Form.Item name={[prefix, "latitude"]} hidden
                       rules={[{
                           validator: (_, value) =>
                               value && value !== 0
                                   ? Promise.resolve()
                                   : Promise.reject(new Error("Vui lòng chọn địa chỉ từ gợi ý để xác định tọa độ")),
                       }]}
            >
                <Input/>
            </Form.Item>

            <Form.Item name={[prefix, "longitude"]} hidden
                       rules={[{
                           validator: (_, value) =>
                               value && value !== 0
                                   ? Promise.resolve()
                                   : Promise.reject(new Error("Vui lòng chọn địa chỉ từ gợi ý để xác định tọa độ")),
                       }]}
            >
                <Input/>
            </Form.Item>

            <Form.Item name={[prefix, "cityName"]} hidden
                       rules={[{
                           validator: (_, value) =>
                               value && value.trim() !== ""
                                   ? Promise.resolve()
                                   : Promise.reject(new Error("Thiếu tên thành phố")),
                       }]}
            >
                <Input/>
            </Form.Item>

            <Form.Item name={[prefix, "wardName"]} hidden
                       rules={[{
                           validator: (_, value) =>
                               value && value.trim() !== ""
                                   ? Promise.resolve()
                                   : Promise.reject(new Error("Thiếu tên phường/xã")),
                       }]}
            >
                <Input/>
            </Form.Item>
        </>
    );
};

export default AddressForm;