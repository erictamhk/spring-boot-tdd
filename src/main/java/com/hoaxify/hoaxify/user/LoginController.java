package com.hoaxify.hoaxify.user;

import com.hoaxify.hoaxify.shared.GenericResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
public class LoginController {
    @PostMapping("/api/1.0/login")
    void handleLogin() {
        
    }

}
