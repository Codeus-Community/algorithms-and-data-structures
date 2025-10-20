package com.codeus.inbox.postorderservice.repository;

import com.codeus.inbox.postorderservice.entity.InboxEvent;
import com.codeus.inbox.postorderservice.entity.InboxEventStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface InboxEventRepository extends JpaRepository<InboxEvent, UUID> {

    List<InboxEvent> findTop20ByStatusOrderByReceivedAtAsc(InboxEventStatus status);
}
