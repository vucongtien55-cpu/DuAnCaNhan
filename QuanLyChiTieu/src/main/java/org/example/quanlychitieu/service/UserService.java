package org.example.quanlychitieu.service;

import org.example.quanlychitieu.entity.User;
import org.example.quanlychitieu.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;

    @Autowired
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    //Xác thực người dùng khi đăng nhập

    //Xác thực người dùng khi đăng nhập qua email và mật khẩu

    public Optional<User> authenticate(String email, String password) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isPresent() && userOpt.get().getPassword().equals(password)) {
            return userOpt;
        }
        return Optional.empty();
    }

    //Đăng ký tài khoản người dùng mới

    //Đăng ký tài khoản người dùng mới

    public User register(User user) {
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new IllegalArgumentException("Email is already registered!");
        }
        return userRepository.save(user);
    }
    //Tìm người dùng theo email


    //Tìm người dùng theo emaill

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }
}
