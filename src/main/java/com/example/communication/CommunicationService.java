package com.example.communication;

import com.example.user.User;
import com.example.security.SecurityService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class CommunicationService {

    private final CommunicationRepository communicationRepository;
    private final CccRepository cccRepository;
    private final SecurityService securityService;

    public CommunicationService(CommunicationRepository communicationRepository, CccRepository cccRepository,
            SecurityService securityService) {
        this.communicationRepository = communicationRepository;
        this.cccRepository = cccRepository;
        this.securityService = securityService;
    }

    public Communication createCommunication(Long cccId, String subject, CommunicationType type, User createdBy) {
        Ccc ccc = cccRepository.findById(cccId)
                .orElseThrow(() -> new IllegalArgumentException("CCC not found with id: " + cccId));

        if (!securityService.isProjectDirector(ccc.getProject().getId())) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Only Project Director can create communications");
        }

        Communication communication = new Communication();
        communication.setCcc(ccc);
        communication.setSubject(subject);
        communication.setType(type);
        communication.setCreatedBy(createdBy);
        communication.setStatus(CommunicationStatus.OPEN);

        Communication savedCommunication = communicationRepository.save(communication);

        return savedCommunication;
    }

    @Transactional(readOnly = true)
    public List<Communication> getAllByCcc(Long cccId) {
        return communicationRepository.findAllByCccIdOrderByUpdatedAtDesc(cccId);
    }

    @Transactional(readOnly = true)
    public Optional<Communication> findById(Long id) {
        return communicationRepository.findByIdWithDetails(id);
    }

    public Communication updateStatus(Long communicationId, CommunicationStatus status) {
        Communication communication = communicationRepository.findById(communicationId)
                .orElseThrow(() -> new IllegalArgumentException("Communication not found with id: " + communicationId));

        Long projectId = communication.getCcc().getProject().getId();
        if (!securityService.isProjectDirectorOrSponsor(projectId)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Only Project Director or Sponsor can update status");
        }

        communication.setStatus(status);
        return communicationRepository.save(communication);
    }

    public void delete(Long communicationId) {
        Communication communication = communicationRepository.findById(communicationId)
                .orElseThrow(() -> new IllegalArgumentException("Communication not found with id: " + communicationId));

        Long projectId = communication.getCcc().getProject().getId();
        if (!securityService.isProjectDirector(projectId)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Only Project Director can delete communications");
        }

        communicationRepository.deleteById(communicationId);
    }
}
