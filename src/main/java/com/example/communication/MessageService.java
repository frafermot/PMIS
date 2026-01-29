package com.example.communication;

import com.example.user.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@Transactional
public class MessageService {

    private final MessageRepository messageRepository;
    private final CommunicationRepository communicationRepository;

    public MessageService(MessageRepository messageRepository, CommunicationRepository communicationRepository) {
        this.messageRepository = messageRepository;
        this.communicationRepository = communicationRepository;
    }

    public Message sendMessage(Long communicationId, User sender, String content) {
        Communication communication = communicationRepository.findById(communicationId)
                .orElseThrow(() -> new IllegalArgumentException("Communication not found with id: " + communicationId));

        Message message = new Message();
        message.setCommunication(communication);
        message.setSender(sender);
        message.setContent(content);

        // Update communication updated_at to bring it to the top
        communication.setUpdatedAt(java.time.LocalDateTime.now());
        communicationRepository.save(communication);

        return messageRepository.save(message);
    }

    @Transactional(readOnly = true)
    public List<Message> getAllByCommunication(Long communicationId) {
        return messageRepository.findAllByCommunicationIdWithDetails(communicationId);
    }

    public void delete(Long messageId) {
        messageRepository.deleteById(messageId);
    }
}
