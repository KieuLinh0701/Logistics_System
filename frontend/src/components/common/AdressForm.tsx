import React, { useEffect, useRef, useState } from "react";
import { Form, Input, Select } from "antd";
import type { City, Ward } from "../../types/location";
import locationApi from "../../api/locationApi";
import { getPlaceDetails } from "../../service/mapsService";
import type { Prediction } from "../../service/mapsService";

const { Option } = Select;

interface AddressFormProps {
    form: any;
    prefix: string;
    initialCity?: number;
    initialWard?: number;
    initialDetail?: string;
    disableCity?: boolean;
    disableWard?: boolean;
    disableDetailAddress?: boolean;
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
                                                 }) => {
    const [cities, setCities] = useState<City[]>([]);
    const [wards, setWards] = useState<Ward[]>([]);
    const [suggestions, setSuggestions] = useState<Prediction[]>([]);
    const [addressInput, setAddressInput] = useState("");
    const [isFirstLoad, setIsFirstLoad] = useState(true);

    const autocompleteServiceRef = useRef<google.maps.places.AutocompleteService | null>(null);
    const debounceRef = useRef<ReturnType<typeof setTimeout> | null>(null);

    const selectedCity = Form.useWatch([prefix, "cityCode"], form);

    // Load Google Maps + Places nếu chưa có
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

    // Load cities 1 lần
    useEffect(() => {
        locationApi.getCities().then(setCities).catch(console.error);
    }, []);

    // Set initial detail
    useEffect(() => {
        if (initialDetail) {
            form.setFieldsValue({
                [prefix]: { ...form.getFieldValue(prefix), detail: initialDetail },
            });
        }
    }, [initialDetail, form, prefix]);

    // Load wards khi city thay đổi
    useEffect(() => {
        const cityCode = selectedCity || initialCity;
        if (!cityCode) return;

        locationApi.getWardsByCity(Number(cityCode)).then((wardList) => {
            setWards(wardList);
            if (initialWard && isFirstLoad) {
                form.setFieldsValue({
                    [prefix]: { ...form.getFieldValue(prefix), wardCode: initialWard },
                });
                setIsFirstLoad(false);
            }
        });
    }, [selectedCity, initialCity]);

    // Chuẩn hóa string: bỏ dấu, lowercase, bỏ prefix hành chính
    const normalizeStr = (str: string) =>
        str
            .normalize("NFD")
            .replace(/[\u0300-\u036f]/g, "")
            .toLowerCase()
            .trim()
            .replace(
                /^(thanh pho |tinh |thi xa |quan |huyen |phuong |xa |thi tran |tp\.? ?)/,
                ""
            )
            .trim();

    // Gợi ý địa chỉ từ Google
    const handleAddressInput = (val: string) => {
        setAddressInput(val);
        if (debounceRef.current) clearTimeout(debounceRef.current);
        if (val.length < 3) return setSuggestions([]);

        debounceRef.current = setTimeout(() => {
            if (!autocompleteServiceRef.current) return;

            autocompleteServiceRef.current.getPlacePredictions(
                {
                    input: val,
                    language: "vi",
                    componentRestrictions: { country: "vn" },
                },
                (predictions, status) => {
                    if (
                        status === window.google.maps.places.PlacesServiceStatus.OK &&
                        predictions
                    ) {
                        setSuggestions(
                            predictions.map((p) => ({
                                place_id: p.place_id,
                                description: p.description,
                            }))
                        );
                    } else {
                        setSuggestions([]);
                    }
                }
            );
        }, 300);
    };

    // Match ward theo thứ tự ưu tiên: exact > DB-in-query > query-in-DB (tránh false positive A/B)
    const matchWard = (wardList: Ward[], wardNameToMatch: string): Ward | undefined => {
        const normalizedQuery = normalizeStr(wardNameToMatch);
        if (!normalizedQuery) return undefined;

        // 1. Exact match
        const exact = wardList.find((w) => normalizeStr(w.name) === normalizedQuery);
        if (exact) return exact;

        // 2. Query chứa tên DB (vd: query="tang nhon phu a", DB="tang nhon phu a")
        const queryContainsDb = wardList.find(
            (w) => normalizedQuery.includes(normalizeStr(w.name)) && normalizeStr(w.name).length > 3
        );
        if (queryContainsDb) return queryContainsDb;

        // 3. DB chứa query — chỉ dùng khi không có ward nào cùng prefix (tránh nhầm A/B)
        const siblings = wardList.filter((w) => normalizeStr(w.name).startsWith(normalizedQuery));
        if (siblings.length === 1) return siblings[0];

        return undefined;
    };

    // Khi chọn gợi ý → parse address_components → map về cityCode/wardCode
    const handleSelectSuggestion = async (prediction: Prediction) => {
        setAddressInput(prediction.description);
        setSuggestions([]);

        try {
            const data = await getPlaceDetails(prediction.place_id);
            const components: {
                long_name: string;
                short_name: string;
                types: string[] }[] = data?.result?.address_components || [];

            const formattedAddress: string = data?.result?.formatted_address || "";

            // Lấy tên tỉnh/thành phố (level 1)
            const provinceName =
                components.find((c) => c.types.includes("administrative_area_level_1"))
                    ?.long_name || "";

            // Lấy quận/huyện (level 2)
            const districtName =
                components.find((c) => c.types.includes("administrative_area_level_2"))
                    ?.long_name || "";

            const level3 =
                components.find((c) => c.types.includes("administrative_area_level_3"))
                    ?.long_name || "";
            const subLocality1 =
                components.find((c) => c.types.includes("sublocality_level_1"))?.long_name || "";
            const subLocality2 =
                components.find((c) => c.types.includes("sublocality_level_2"))?.long_name || "";
            const locality =
                components.find((c) => c.types.includes("locality"))?.long_name || "";

            // Khi address_components không có phường (Google thiếu data),
            // fallback parse từ formatted_address: "96a Đường 6, Tăng Nhơn Phú, Hồ Chí Minh"
            // tách phần giữa dấu phẩy đầu và tỉnh/thành để lấy tên phường
            const wardFromFormatted = (() => {
                if (!formattedAddress) return "";
                // formatted_address thường có dạng: "số nhà đường, Phường, Quận, Thành phố"
                // Bỏ phần tỉnh/thành (cuối) và lấy token thứ 2 từ phải (phường/xã)
                const parts = formattedAddress.split(",").map((p) => p.trim());
                // Loại bỏ phần cuối là tỉnh/thành và "Việt Nam"
                const filtered = parts.filter(
                    (p) =>
                        !normalizeStr(p).includes(normalizeStr(provinceName)) &&
                        !normalizeStr(p).includes("viet nam") &&
                        !normalizeStr(p).includes("vietnam")
                );
                // Phần tử cuối của filtered thường là phường/quận
                return filtered[filtered.length - 1] || "";
            })();

            // Ưu tiên: level_3 > sublocality > wardFromFormatted > districtName > locality
            const wardNameToMatch =
                level3 || subLocality1 || subLocality2 || wardFromFormatted || districtName || locality;

            // Số nhà + tên đường cho field detail
            const streetNumber =
                components.find((c) => c.types.includes("street_number"))?.long_name || "";
            const route =
                components.find((c) => c.types.includes("route"))?.long_name || "";
            const detail = [streetNumber, route].filter(Boolean).join(" ");

            // --- Match city ---
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

            // --- Load wards của city match được ---
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

    // Khi user tự đổi city thủ công
    const handleCityChange = (newCity: number) => {
        const cityName = cities.find(c => c.code === newCity)?.name ?? "";

        form.setFieldsValue({
            [prefix]: {
                ...form.getFieldValue(prefix),
                wardCode: undefined,
                wardName: "",
                cityName,
            },
        });
        setWards([]);
        setIsFirstLoad(false);
        locationApi.getWardsByCity(newCity).then(setWards).catch(console.error);
    };

    return (
        <>
            {/* Ô tìm địa chỉ nhanh - ẩn khi disableCity */}
            {!disableCity && (
                <Form.Item label={<span className="modal-lable">Tìm địa chỉ nhanh</span>}>
                    <div style={{ position: "relative" }}>
                        <Input
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
                            <ul
                                style={{
                                    position: "absolute",
                                    zIndex: 1000,
                                    background: "#fff",
                                    border: "1px solid #d9d9d9",
                                    borderRadius: 6,
                                    padding: 0,
                                    margin: "4px 0 0 0",
                                    listStyle: "none",
                                    width: "100%",
                                    boxShadow: "0 4px 12px rgba(0,0,0,0.12)",
                                    maxHeight: 240,
                                    overflowY: "auto",
                                }}
                            >
                                {suggestions.map((s) => (
                                    <li
                                        key={s.place_id}
                                        onClick={() => handleSelectSuggestion(s)}
                                        style={{
                                            padding: "8px 12px",
                                            cursor: "pointer",
                                            borderBottom: "1px solid #f0f0f0",
                                            fontSize: 13,
                                        }}
                                        onMouseEnter={(e) =>
                                            (e.currentTarget.style.background = "#f5f5f5")
                                        }
                                        onMouseLeave={(e) =>
                                            (e.currentTarget.style.background = "#fff")
                                        }
                                    >
                                        {s.description}
                                    </li>
                                ))}
                            </ul>
                        )}
                    </div>
                </Form.Item>
            )}

            {/* Tỉnh / Thành phố */}
            <Form.Item
                name={[prefix, "cityCode"]}
                label={<span className="modal-lable">Tỉnh / Thành phố</span>}
                rules={[{ required: true, message: "Vui lòng chọn tỉnh / thành phố!" }]}
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
                        <Option key={c.code} value={c.code} label={c.name}>
                            {c.name}
                        </Option>
                    ))}
                </Select>
            </Form.Item>

            {/* Phường / Xã */}
            <Form.Item
                name={[prefix, "wardCode"]}
                label={<span className="modal-lable">Phường / Xã</span>}
                rules={[{ required: true, message: "Vui lòng chọn phường / xã!" }]}
            >
                <Select
                    onChange={(value) => {
                        const wardName = wards.find(w => w.code === value)?.name ?? "";
                        form.setFieldsValue({
                            [prefix]: { ...form.getFieldValue(prefix), wardName },
                        });
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
                        <Option key={w.code} value={w.code} label={w.name}>
                            {w.name}
                        </Option>
                    ))}
                </Select>
            </Form.Item>

            {/* Chi tiết */}
            <Form.Item
                name={[prefix, "detail"]}
                label={<span className="modal-lable">Chi tiết</span>}
                rules={[{ required: true, message: "Vui lòng nhập số nhà, tên đường!" }]}
            >
                <Input
                    className="modal-custom-input"
                    placeholder="Số nhà, tên đường..."
                    disabled={disableDetailAddress}
                />
            </Form.Item>

            <Form.Item name={[prefix, "latitude"]} hidden><Input /></Form.Item>
            <Form.Item name={[prefix, "longitude"]} hidden><Input /></Form.Item>
            <Form.Item name={[prefix, "cityName"]} hidden><Input /></Form.Item>
            <Form.Item name={[prefix, "wardName"]} hidden><Input /></Form.Item>
        </>
    );
};

export default AddressForm;