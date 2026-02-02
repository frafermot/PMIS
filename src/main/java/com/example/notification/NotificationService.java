package com.example.notification;

import com.example.user.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    public void notify(User recipient, String content, String link) {
        if (recipient == null)
            return;
        Notification notification = new Notification();
        notification.setRecipient(recipient);
        notification.setContent(content);
        notification.setLink(link);
        notificationRepository.save(notification);
    }

    public void notify(User recipient, String content) {
        notify(recipient, content, null);
    }

    @Transactional(readOnly = true)
    public List<Notification> getUserNotifications(Long userId) {
        return notificationRepository.findAllByRecipientIdOrderByCreatedAtDesc(userId);
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(Long userId) {
        return notificationRepository.countByRecipientIdAndIsReadFalse(userId);
    }

    public void markAsRead(Long notificationId) {
        Optional<Notification> notification = notificationRepository.findById(notificationId);
        notification.ifPresent(n -> {
            n.setRead(true);
            notificationRepository.save(n);
        });
    }
}
