package com.example.portfolio;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PortfolioService {

    private final PortfolioRepository portfolioRepository;

    public PortfolioService(PortfolioRepository portfolioRepository) {
        this.portfolioRepository = portfolioRepository;
    }

    public Portfolio createOrUpdate(Portfolio portfolio) {
        return portfolioRepository.save(portfolio);
    }

    public Portfolio get(Long id) {
        return portfolioRepository.findById(id).orElse(null);
    }

    public void delete(Long id) {
        portfolioRepository.deleteById(id);
    }

    public List<Portfolio> getAll() {
        return portfolioRepository.findAll();
    }
}
