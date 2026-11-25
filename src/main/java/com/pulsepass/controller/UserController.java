package com.pulsepass.controller;

import com.pulsepass.dto.request.RegisterUserRequest;
import com.pulsepass.dto.response.UserResponse;
import com.pulsepass.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// Contrato HTTP: PRD Controladores 8.4 (FR-CTRL-USR-001, FR-CTRL-USR-002, FR-CTRL-USR-003)
@RestController
@RequestMapping("/api/users")
public class UserController {

    // Depende solo del Service (CTRL-002, NFR-CTRL-004)
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping//Indica que el método recibe peticiones HTTP POST.
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterUserRequest request) {//indica que Spring debe tomar el contenido JSON enviado por el cliente y convertirlo en un objeto Java de tipo RegisterUserRequest.
        UserResponse created = userService.register(request);//@Valid Esta anotación ejecuta las validaciones definidas en RegisterUserRequest.
        return ResponseEntity.status(HttpStatus.CREATED).body(created);//devuelve 201 created
    }

    // FR-CTRL-USR-002. /api/users/by-email?email=...
    @GetMapping("/by-email")
    public ResponseEntity<UserResponse> findByEmail(@RequestParam String email) {
        return ResponseEntity.ok(userService.findByEmail(email));
    }

    // FR-CTRL-USR-003. /api/users/by-username?username=...
    @GetMapping("/by-username")
    public ResponseEntity<UserResponse> findByUsername(@RequestParam String username) {
        return ResponseEntity.ok(userService.findByUsername(username));
    }
}
