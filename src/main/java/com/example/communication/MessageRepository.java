package com.example.communication;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MessageRepository extends JpaRepository<Message, Long> {

    @Query("SELECT m FROM Message m JOIN FETCH m.sender WHERE m.communication.id = :communicationId ORDER BY m.sentAt ASC")
    List<Message> findAllByCommunicationIdWithDetails(@Param("communicationId") Long communicationId);

    List<Message> findAllByCommunicationIdOrderBySentAtAsc(Long communicationId);
}
