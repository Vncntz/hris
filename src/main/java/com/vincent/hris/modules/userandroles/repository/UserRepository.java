package com.vincent.hris.modules.userandroles.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.vincent.hris.modules.userandroles.model.User;

public interface UserRepository extends JpaRepository<User, Long> {
    
    User findByUsername(String username);
}
