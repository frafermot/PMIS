package com.example.user;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User createOrUpdate(User user) {
        return userRepository.save(user);
    }

    public User get(Long userId) {
        return userRepository.findById(userId).orElse(null);
    }

    public void delete(Long userId) {
        userRepository.deleteById(userId);
    }

    public List<User> getAll() {
        List<User> users = userRepository.findAllWithProject();
        return users;
    }

    public List<User> findAllByRole(Role role) {
        return userRepository.findAllByRole(role);
    }

    public User findByUvus(String uvus) {
        return userRepository.findByUvus(uvus);
    }
}
