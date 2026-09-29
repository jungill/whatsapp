package com.junior.chat.repository;

import com.junior.chat.model.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<UserEntity, String> {}
//                                                      entité      type d'itendifiant