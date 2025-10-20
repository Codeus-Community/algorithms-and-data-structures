package com.codeus.outbox.orderservice.repository;

import com.codeus.outbox.orderservice.entity.OutboxEvent;
import com.codeus.outbox.orderservice.entity.OutboxEventStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    List<OutboxEvent> findTop10ByStatusOrderByCreatedAtAsc(OutboxEventStatus status);
}
