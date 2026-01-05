package com.example.portfolio;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PortfolioService {

    private final PortfolioRepository portfolioRepository;

    public PortfolioService(PortfolioRepository portfolioRepository) {
        this.portfolioRepository = portfolioRepository;
    }

    // Find all portfolios with details from the repository

    public Portfolio createOrUpdate(Portfolio portfolio) {
        return portfolioRepository.save(portfolio);
    }

    public Portfolio get(Long id) {
        return portfolioRepository.findById(id).orElse(null);
    }

    public void delete(Long id) {
        portfolioRepository.deleteById(id);
    }
}
