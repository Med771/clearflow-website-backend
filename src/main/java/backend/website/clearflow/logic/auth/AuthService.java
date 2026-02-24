package backend.website.clearflow.logic.auth;

import backend.website.clearflow.logic.auth.dto.AuthLoginRequest;
import backend.website.clearflow.logic.auth.dto.AuthTokensResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface AuthService {
    AuthTokensResponse login(AuthLoginRequest request, HttpServletResponse response);

    AuthTokensResponse refresh(HttpServletRequest request, HttpServletResponse response);

    void logout(HttpServletRequest request, HttpServletResponse response);
}
