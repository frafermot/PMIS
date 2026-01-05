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
        return userRepository.findAll();
    }
}
