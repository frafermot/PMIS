package com.example.communication;

import com.example.project.Project;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;

@Service
@Transactional
public class CccService {

    private final CccRepository cccRepository;

    public CccService(CccRepository cccRepository) {
        this.cccRepository = cccRepository;
    }

    public Ccc createCccForProject(Project project) {
        Ccc ccc = new Ccc();
        ccc.setProject(project);
        return cccRepository.save(ccc);
    }

    @Transactional(readOnly = true)
    public Optional<Ccc> getCccByProject(Long projectId) {
        return cccRepository.findByProjectId(projectId);
    }

    @Transactional(readOnly = true)
    public Optional<Ccc> findById(Long cccId) {
        Optional<Ccc> ccc = cccRepository.findById(cccId);
        // Force initialization of the proxy to avoid LazyInitializationException in the
        // view
        ccc.ifPresent(c -> c.getProject().getName());
        return ccc;
    }

    public void delete(Long cccId) {
        cccRepository.deleteById(cccId);
    }
}
