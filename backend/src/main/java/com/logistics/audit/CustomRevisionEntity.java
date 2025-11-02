package com.logistics.audit;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import org.hibernate.envers.RevisionEntity;
import org.hibernate.envers.RevisionNumber;
import org.hibernate.envers.RevisionTimestamp;

@Entity
@Getter
@Setter
@Table(name = "revinfo")
@RevisionEntity(UserRevisionListener.class)
public class CustomRevisionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @RevisionNumber
    private int id;

    @RevisionTimestamp
    private long timestamp;

    private Integer userId;
}