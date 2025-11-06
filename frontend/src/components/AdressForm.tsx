import React, { useEffect, useState, useRef } from "react";
import { Form, Input, Select } from "antd";
import axios from "axios";

const { Option } = Select;

interface Province {
  code: number;
  name: string;
}

interface Commune {
  code: number;
  name: string;
}

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
  const [provinces, setProvinces] = useState<Province[]>([]);
  const [communes, setCommunes] = useState<Commune[]>([]);
  const [isFirstLoad, setIsFirstLoad] = useState(true);
  
  // SỬA: Khởi tạo useRef với giá trị undefined
  const prevProvinceRef = useRef<number | undefined>(undefined);

  // 🆕 Watch province theo prefix
  const selectedProvince = Form.useWatch([prefix, "province"], form);

  // Lấy danh sách tỉnh/thành phố
  useEffect(() => {
    axios
      .get<Province[]>("https://provinces.open-api.vn/api/v2/p/")
      .then((res) => setProvinces(res.data))
      .catch((err) => console.error(err));
  }, []);

  useEffect(() => {
    if (initialDetail) {
      form.setFieldsValue({
        [prefix]: {
          ...form.getFieldValue(prefix),
          address: initialDetail,
        },
      });
    }
  }, [initialDetail, form, prefix]);

  interface ProvinceDetail {
    name: string;
    code: number;
    wards: {
      code: number;
      name: string;
      province_code: number;
    }[];
  }

  useEffect(() => {
    const provinceCode = selectedProvince || initialCity;
    
    if (provinceCode) {
      axios.get<ProvinceDetail>(`https://provinces.open-api.vn/api/v2/p/${provinceCode}?depth=2`)
        .then((res) => {
          const wards: Commune[] = (res.data.wards || []).map(w => ({ code: w.code, name: w.name }));
          setCommunes(wards);

          // ✅ Nếu có initialWard VÀ đang là lần đầu load, set commune
          if (initialWard && isFirstLoad) {
            form.setFieldsValue({
              [prefix]: {
                ...form.getFieldValue(prefix),
                commune: initialWard,
              }
            });
            setIsFirstLoad(false);
          }

        })
        .catch(err => console.error(err));
    } else {
      // 🔄 Reset communes khi không có province
      setCommunes([]);
    }
  }, [selectedProvince, initialCity, initialWard, form, prefix, isFirstLoad]);

  // 🔄 QUAN TRỌNG: Reset ward khi province thay đổi (chỉ khi KHÔNG phải lần đầu)
  useEffect(() => {
    if (selectedProvince && selectedProvince !== prevProvinceRef.current) {
      // Chỉ reset khi province thực sự thay đổi, không phải lần đầu load
      if (!isFirstLoad && prevProvinceRef.current !== undefined) {
        form.setFieldsValue({
          [prefix]: {
            ...form.getFieldValue(prefix),
            commune: undefined,
          }
        });
      }
      
      // Clear communes list (sẽ được set lại trong useEffect trên)
      setCommunes([]);
      prevProvinceRef.current = selectedProvince;
    }
  }, [selectedProvince, form, prefix, isFirstLoad]);

  // Xử lý khi người dùng thay đổi province thủ công
  const handleProvinceChange = (newProvince: number) => {
    // Reset commune khi người dùng chọn province mới
    form.setFieldsValue({
      [prefix]: {
        ...form.getFieldValue(prefix),
        commune: undefined,
      }
    });
    setCommunes([]);
    setIsFirstLoad(false);
  };

  return (
    <>
      <Form.Item
        name={[prefix, "province"]}
        label="Tỉnh / Thành phố"
        rules={[{ required: true, message: "Chọn tỉnh / thành phố!" }]}
      >
        <Select
          showSearch
          placeholder="Chọn tỉnh/thành phố"
          optionFilterProp="label"
          disabled={disableCity}
          onChange={handleProvinceChange}
          filterOption={(input, option) =>
            (option?.label as string)
              .toLowerCase()
              .includes(input.toLowerCase())
          }
        >
          {provinces.map((p) => (
            <Option key={p.code} value={p.code} label={p.name}>
              {p.name}
            </Option>
          ))}
        </Select>
      </Form.Item>

      <Form.Item
        name={[prefix, "commune"]}
        label="Phường / Xã"
        rules={[{ required: true, message: "Chọn phường / xã!" }]}
      >
        <Select
          showSearch
          placeholder={"Chọn phường/xã"}
          optionFilterProp="label"
          filterOption={(input, option) =>
            (option?.label as string)
              .toLowerCase()
              .includes(input.toLowerCase())
          }
          disabled={!selectedProvince || disableWard}
        >
          {communes.map((c) => (
            <Option key={c.code} value={c.code} label={c.name}>
              {c.name}
            </Option>
          ))}
        </Select>
      </Form.Item>

      <Form.Item
        name={[prefix, "address"]}
        label="Chi tiết"
        rules={[{ required: true, message: "Nhập số nhà, tên đường!" }]}
      >
        <Input placeholder="Số nhà, tên đường..." disabled={disableDetailAddress} />
      </Form.Item>
    </>
  );
};

export default AddressForm;