package backend.website.clearflow.logic.auth;

import backend.website.clearflow.logic.auth.dto.AuthLoginRequest;
import backend.website.clearflow.logic.auth.dto.AuthTokensResponse;
import backend.website.clearflow.logic.auth.dto.RegisterSellerRequest;
import backend.website.clearflow.logic.auth.dto.RegisterSellerResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface AuthService {
    RegisterSellerResponse registerSeller(RegisterSellerRequest request);

    AuthTokensResponse login(AuthLoginRequest request, HttpServletResponse response);

    AuthTokensResponse refresh(HttpServletRequest request, HttpServletResponse response);

    void logout(HttpServletRequest request, HttpServletResponse response);
}
