package com.hoaxify.hoaxify.user;

import com.hoaxify.hoaxify.shared.CurrentUser;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.Map;

@RestController
public class LoginController {
    @PostMapping("/api/1.0/login")
    Map<String, Object> handleLogin(@CurrentUser User loggedInUser) {
        return Collections.singletonMap("id", loggedInUser.getId());
    }
}
