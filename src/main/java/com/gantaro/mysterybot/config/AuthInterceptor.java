package com.gantaro.mysterybot.config;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
            Object handler) throws Exception {
        HttpSession session = request.getSession(false);
        String path = request.getServletPath();
        String loginGroupId =
                session != null ? (String) session.getAttribute("loginGroupId") : null;

        if (path.startsWith("/admin/")) {
            String originalAdminId =
                    session != null ? (String) session.getAttribute("originalAdminId") : null;
            if (!"admin".equals(loginGroupId)
                    && !(path.equals("/admin/end-impersonate")
                            && "admin".equals(originalAdminId))) {
                redirectToLogin(request, response);
                return false;
            }
        } else if (path.startsWith("/user/")) {
            if (loginGroupId == null) {
                redirectToLogin(request, response);
                return false;
            }
        }
        return true;
    }

    private void redirectToLogin(HttpServletRequest request, HttpServletResponse response)
            throws Exception {
        response.sendRedirect(response.encodeRedirectURL(request.getContextPath() + "/auth/login"));
    }
}
