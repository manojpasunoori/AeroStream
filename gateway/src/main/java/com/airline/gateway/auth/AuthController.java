package com.airline.gateway.auth;

import com.airline.gateway.security.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final JwtTokenProvider jwtTokenProvider;
    private final String configuredUsername;
    private final String configuredPassword;

    public AuthController(
            JwtTokenProvider jwtTokenProvider,
            @Value("${security.auth.username}") String configuredUsername,
            @Value("${security.auth.password}") String configuredPassword
    ) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.configuredUsername = configuredUsername;
        this.configuredPassword = configuredPassword;
    }

    @PostMapping("/login")
    public Mono<LoginResponse> login(@RequestBody Mono<LoginRequest> requestMono) {
        return requestMono.map(request -> {
            if (!configuredUsername.equals(request.username()) || !configuredPassword.equals(request.password())) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
            }

            String token = jwtTokenProvider.generateToken(request.username());
            return new LoginResponse(token, "Bearer");
        });
    }

    public record LoginRequest(String username, String password) {
    }

    public record LoginResponse(String accessToken, String tokenType) {
    }
}
