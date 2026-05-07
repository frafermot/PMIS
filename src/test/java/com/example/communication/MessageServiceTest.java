package com.example.communication;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.util.Optional;
import java.util.List;
import java.util.ArrayList;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.example.notification.NotificationService;
import com.example.project.Project;
import com.example.user.User;

class MessageServiceTest {

    @Mock
    private MessageRepository messageRepository;
    @Mock
    private CommunicationRepository communicationRepository;
    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private MessageService messageService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testSendMessage() {
        User director = new User(); director.setId(1L); director.setName("Dir");
        User sponsor = new User(); sponsor.setId(2L); sponsor.setName("Sponsor");
        
        Project project = new Project();
        project.setDirector(director);
        project.setSponsor(sponsor);
        
        Ccc ccc = new Ccc();
        ccc.setProject(project);
        
        Communication comm = new Communication();
        comm.setId(10L);
        comm.setCcc(ccc);
        comm.setSubject("Test Comm");
        
        when(communicationRepository.findByIdWithDetails(10L)).thenReturn(Optional.of(comm));
        
        Message savedMessage = new Message();
        savedMessage.setId(100L);
        when(messageRepository.save(any(Message.class))).thenReturn(savedMessage);
        
        Message result = messageService.sendMessage(10L, director, "Hello");
        
        assertNotNull(result);
        verify(communicationRepository).save(comm);
        verify(messageRepository).save(any(Message.class));
        verify(notificationService).notify(eq(sponsor), anyString(), anyString());
    }

    @Test
    void testGetAllByCommunication() {
        List<Message> list = new ArrayList<>();
        list.add(new Message());
        when(messageRepository.findAllByCommunicationIdWithDetails(1L)).thenReturn(list);
        
        List<Message> result = messageService.getAllByCommunication(1L);
        assertEquals(1, result.size());
    }

    @Test
    void testDelete() {
        messageService.delete(1L);
        verify(messageRepository).deleteById(1L);
    }
}
