import React, { useEffect, useState, useRef } from "react";
import { Form, Input, Select } from "antd";
import type { City, Ward } from "../../types/location";
import locationApi from "../../api/locationApi";

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
  const [isFirstLoad, setIsFirstLoad] = useState(true);

  const prevCityRef = useRef<number | undefined>(undefined);

  const selectedCity = Form.useWatch([prefix, "cityCode"], form);

  // Lấy danh sách city
  useEffect(() => {
    const fetchCities = async () => {
      try {
        const res = await locationApi.getCities();
        setCities(res);
      } catch (err) {
        console.error("Lỗi lấy danh sách tỉnh/thành:", err);
      }
    };
    fetchCities();
  }, []);

  // Set initial detail nếu có
  useEffect(() => {
    if (initialDetail) {
      form.setFieldsValue({
        [prefix]: {
          ...form.getFieldValue(prefix),
          detail: initialDetail,
        },
      });
    }
  }, [initialDetail, form, prefix]);

  // Lấy ward theo city
  useEffect(() => {
    const cityCode = selectedCity || initialCity;

    const fetchWards = async () => {
      if (!cityCode) {
        setWards([]);
        return;
      }

      try {
        const wardList = await locationApi.getWardsByCity(Number(cityCode));
        setWards(wardList);

        // set initial ward lần đầu
        if (initialWard && isFirstLoad) {
          form.setFieldsValue({
            [prefix]: {
              ...form.getFieldValue(prefix),
              wardCode: initialWard,
            },
          });
          setIsFirstLoad(false);
        }
      } catch (err) {
        console.error("Lỗi lấy phường/xã:", err);
      }
    };

    fetchWards();
  }, [selectedCity, initialCity, initialWard, form, prefix, isFirstLoad]);

  // Reset ward nếu người dùng đổi city (không phải lần đầu)
  useEffect(() => {
    if (selectedCity && selectedCity !== prevCityRef.current) {
      if (!isFirstLoad && prevCityRef.current !== undefined) {
        form.setFieldsValue({
          [prefix]: {
            ...form.getFieldValue(prefix),
            wardCode: undefined,
          },
        });
      }

      setWards([]);
      prevCityRef.current = selectedCity;
    }
  }, [selectedCity, form, prefix, isFirstLoad]);

  // khi người dùng tự chọn city
  const handleCityChange = (newCity: number) => {
    form.setFieldsValue({
      [prefix]: {
        ...form.getFieldValue(prefix),
        wardCode: undefined,
      },
    });

    setWards([]);
    setIsFirstLoad(false);
  };

  return (
    <>
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
          {
            cities.map((c) => (
              <Option key={c.code} value={c.code} label={c.name} >
                {c.name}
              </Option>
            ))
          }
        </Select>
      </Form.Item>

      < Form.Item
        name={[prefix, "wardCode"]}
        label={<span className="modal-lable">Phường / Xã</span>}
        rules={[{ required: true, message: "Vui lòng chọn phường / xã!" }]}
      >
        <Select
          className="modal-custom-select"
          showSearch
          placeholder="Chọn phường/xã"
          optionFilterProp="label"
          disabled={!selectedCity || disableWard}
          filterOption={(input, option) =>
            (option?.label as string).toLowerCase().includes(input.toLowerCase())
          }
        >
          {
            wards.map((w) => (
              <Option key={w.code} value={w.code} label={w.name} >
                {w.name}
              </Option>
            ))
          }
        </Select>
      </Form.Item>

      < Form.Item
        name={[prefix, "detail"]}
        label={<span className="modal-lable">Chi tiết</span>}
        rules={[{ required: true, message: "Vui lòng nhập số nhà, tên đường!" }]}
      >
        <Input
          className="modal-custom-input"
          placeholder="Số nhà, tên đường..."
          disabled={disableDetailAddress} />
      </Form.Item>
    </>
  );
};

export default AddressForm;