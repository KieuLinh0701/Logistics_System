import React, { useState, useEffect } from "react";
import { Modal, Button, Space } from "antd";
import { translateUserSettlementScheduleWeekday, USER_SETTLEMENT_SCHEDULE_WEEKDAY } from "../../../../utils/userSettlementScheduleUtils";

interface Props {
    visible: boolean;
    initialWeekdays?: string[];
    onCancel: () => void;
    onSave: (selectedWeekdays: string[]) => void;
    loading: boolean;
}

const UserScheduleModal: React.FC<Props> = ({
    visible,
    initialWeekdays = [],
    onCancel,
    onSave,
    loading,
}) => {
    const [selectedDays, setSelectedDays] = useState<string[]>([]);

    useEffect(() => {
        setSelectedDays(initialWeekdays);
    }, [initialWeekdays, visible]);

    const toggleDay = (day: string) => {
        if (selectedDays.includes(day)) {
            setSelectedDays(selectedDays.filter(d => d !== day));
        } else {
            setSelectedDays([...selectedDays, day]);
        }
    };

    return (
        <Modal
            title={
                <span className='modal-title'>
                    Chọn ngày đối soát</span>}
            open={visible}
            onCancel={onCancel}
            onOk={() => onSave(selectedDays)}
            okText="Lưu"
            cancelText="Hủy"
            okButtonProps={{
                className: "modal-ok-button",
                loading: loading
            }}
            cancelButtonProps={{
                className: "modal-cancel-button"
            }}
            width={430}
            className="modal-hide-scrollbar"
        >
            <Space wrap>
                {USER_SETTLEMENT_SCHEDULE_WEEKDAY.map(day => (
                    <Button
                        key={day}
                        className={selectedDays.includes(day) ? "modal-cancel-button" : "default-button"}
                        onClick={() => toggleDay(day)}
                    >

                        {translateUserSettlementScheduleWeekday(day)}
                    </Button>
                ))}
            </Space>
        </Modal>
    );
};

export default UserScheduleModal;