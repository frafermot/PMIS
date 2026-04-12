package com.example.communication;

import com.example.notification.NotificationService;
import com.example.project.Project;
import com.example.user.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@Transactional
public class MessageService {

    private final MessageRepository messageRepository;
    private final CommunicationRepository communicationRepository;
    private final NotificationService notificationService;

    public MessageService(MessageRepository messageRepository, CommunicationRepository communicationRepository,
            NotificationService notificationService) {
        this.messageRepository = messageRepository;
        this.communicationRepository = communicationRepository;
        this.notificationService = notificationService;
    }

    public Message sendMessage(Long communicationId, User sender, String content) {
        Communication communication = communicationRepository.findByIdWithDetails(communicationId)
                .orElseThrow(() -> new IllegalArgumentException("Communication not found with id: " + communicationId));

        Message message = new Message();
        message.setCommunication(communication);
        message.setSender(sender);
        message.setContent(content);

        // Update communication updated_at to bring it to the top
        communication.setUpdatedAt(java.time.LocalDateTime.now());
        communicationRepository.save(communication);

        Message savedMessage = messageRepository.save(message);

        // Send Notification
        // If sender is Director -> Notify Sponsor
        // If sender is Sponsor -> Notify Director
        Project project = communication.getCcc().getProject();
        User director = project.getDirector();
        User sponsor = project.getSponsor();

        if (director != null && sponsor != null) {
            String link = "ccc/communication/" + communication.getId();
            if (sender.getId().equals(director.getId())) {
                notificationService.notify(sponsor,
                        "Nuevo mensaje de " + sender.getName() + " en: " + communication.getSubject(), link);
            } else if (sender.getId().equals(sponsor.getId())) {
                notificationService.notify(director,
                        "Nuevo mensaje de " + sender.getName() + " en: " + communication.getSubject(), link);
            }
        }

        return savedMessage;
    }

    @Transactional(readOnly = true)
    public List<Message> getAllByCommunication(Long communicationId) {
        return messageRepository.findAllByCommunicationIdWithDetails(communicationId);
    }

    public void delete(Long messageId) {
        messageRepository.deleteById(messageId);
    }
}
