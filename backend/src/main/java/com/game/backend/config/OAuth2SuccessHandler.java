package com.game.backend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.game.backend.dto.AuthResponse;
import com.game.backend.service.AuthService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final AuthService authService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        String providerId = oAuth2User.getAttribute("sub");
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");
        String picture = oAuth2User.getAttribute("picture");

        AuthResponse authResponse = authService.oauthLoginOrSignup(
                email,
                name,
                "GOOGLE",
                providerId,
                picture
        );

        String userJson = objectMapper.writeValueAsString(authResponse);

        String redirectUrl = "http://localhost:3000/oauth/success"
                + "?token=" + URLEncoder.encode(authResponse.getToken(), StandardCharsets.UTF_8)
                + "&user=" + URLEncoder.encode(userJson, StandardCharsets.UTF_8);

        response.sendRedirect(redirectUrl);
    }
}
