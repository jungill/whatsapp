package com.junior.chat.web;

import com.junior.chat.model.UserEntity;
import com.junior.chat.repository.UserRepository;
import com.junior.chat.security.JwtService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    public AuthController(UserRepository users, PasswordEncoder encoder, JwtService jwt) {
        this.users = users;
        this.encoder = encoder;
        this.jwt = jwt;
    }

    public record Credentials(String username, String password) {}

    // gère l'inscription d'un utilisateur
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Credentials body) {
        // vérifie s'il n'est pas déjà dans la bdd
        if (users.existsById(body.username())) {
            return ResponseEntity.status(409).body(Map.of("error", "utilisateur deja pris"));
        }
        //l'enregistre dans la bdd
        users.save(new UserEntity(body.username(), encoder.encode(body.password())));
        // retourne le token une fois fois l'utilisateur inscrit
        return ResponseEntity.ok(Map.of("token", jwt.generate(body.username())));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Credentials body) {
        return users.findById(body.username()) // recherche ds la colonne username de la db
                                                // car elle a été marquée avec @Id dans UserEntity
                .filter(u -> encoder.matches(body.password(), u.getPasswordHash()))
                .<ResponseEntity<?>>map(u ->
                        ResponseEntity.ok(Map.of("token", jwt.generate(u.getUsername()))))
                .orElseGet(() -> ResponseEntity.status(401)
                        .body(Map.of("error", "identifiants invalides")));
    }
}