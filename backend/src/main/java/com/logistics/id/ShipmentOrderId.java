package com.logistics.id;

import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;

@Embeddable
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@Getter
@Setter
public class ShipmentOrderId implements Serializable {
    private Integer shipmentId;
    private Integer orderId;
}
