package backend.website.clearflow.logic.auth;

import backend.website.clearflow.logic.auth.dto.AuthLoginRequest;
import backend.website.clearflow.logic.auth.dto.AuthTokensResponse;
import backend.website.clearflow.logic.auth.dto.RegisterSellerRequest;
import backend.website.clearflow.logic.auth.dto.RegisterSellerResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register-seller")
    @ResponseStatus(HttpStatus.CREATED)
    public RegisterSellerResponse registerSeller(@Valid @RequestBody RegisterSellerRequest request) {
        return authService.registerSeller(request);
    }

    @PostMapping("/login")
    public AuthTokensResponse login(@Valid @RequestBody AuthLoginRequest request, HttpServletResponse response) {
        return authService.login(request, response);
    }

    @PostMapping("/refresh")
    public AuthTokensResponse refresh(HttpServletRequest request, HttpServletResponse response) {
        return authService.refresh(request, response);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        authService.logout(request, response);
    }
}
