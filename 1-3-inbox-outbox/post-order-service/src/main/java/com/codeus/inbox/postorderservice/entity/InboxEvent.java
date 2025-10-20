package com.codeus.inbox.postorderservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.Instant;
import java.util.UUID;

@Data
@Entity
@Table(name = "inbox")
@EqualsAndHashCode(of = "id")
public class InboxEvent {

    @Id
    @Column(unique = true, nullable = false)
    private UUID id;

    //todo: add some fields which describe the event
    // payload - JSON

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InboxEventStatus status;

    // other fields
}
