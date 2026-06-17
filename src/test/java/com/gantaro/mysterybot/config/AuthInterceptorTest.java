package com.gantaro.mysterybot.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;

class AuthInterceptorTest {

    private final AuthInterceptor interceptor = new AuthInterceptor();

    @Test
    void redirectsUnauthenticatedUserRequestToLogin() throws Exception {
        MockHttpServletRequest request = request("GET", "/user/dashboard");
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean result = interceptor.preHandle(request, response, new Object());

        assertFalse(result);
        assertEquals("/auth/login", response.getRedirectedUrl());
    }

    @Test
    void allowsAuthenticatedUserRequest() throws Exception {
        MockHttpServletRequest request = request("GET", "/user/dashboard");
        request.setSession(session("event1", null));
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean result = interceptor.preHandle(request, response, new Object());

        assertTrue(result);
        assertNull(response.getRedirectedUrl());
    }

    @Test
    void redirectsNonAdminAdminRequestToLogin() throws Exception {
        MockHttpServletRequest request = request("GET", "/admin/dashboard");
        request.setSession(session("event1", null));
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean result = interceptor.preHandle(request, response, new Object());

        assertFalse(result);
        assertEquals("/auth/login", response.getRedirectedUrl());
    }

    @Test
    void allowsAdminRequestForAdminSession() throws Exception {
        MockHttpServletRequest request = request("GET", "/admin/dashboard");
        request.setSession(session("admin", null));
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean result = interceptor.preHandle(request, response, new Object());

        assertTrue(result);
        assertNull(response.getRedirectedUrl());
    }

    @Test
    void allowsEndImpersonateOnlyWhenOriginalAdminExists() throws Exception {
        MockHttpServletRequest request = request("POST", "/admin/end-impersonate");
        request.setSession(session("event1", "admin"));
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean result = interceptor.preHandle(request, response, new Object());

        assertTrue(result);
        assertNull(response.getRedirectedUrl());
    }

    @Test
    void redirectsEndImpersonateWithoutOriginalAdmin() throws Exception {
        MockHttpServletRequest request = request("POST", "/admin/end-impersonate");
        request.setSession(session("event1", null));
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean result = interceptor.preHandle(request, response, new Object());

        assertFalse(result);
        assertEquals("/auth/login", response.getRedirectedUrl());
    }

    @Test
    void redirectIncludesContextPath() throws Exception {
        MockHttpServletRequest request = request("GET", "/user/dashboard");
        request.setContextPath("/mysterybot");
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean result = interceptor.preHandle(request, response, new Object());

        assertFalse(result);
        assertEquals("/mysterybot/auth/login", response.getRedirectedUrl());
    }

    private MockHttpServletRequest request(String method, String path) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        request.setServletPath(path);
        return request;
    }

    private MockHttpSession session(String loginGroupId, String originalAdminId) {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("loginGroupId", loginGroupId);
        if (originalAdminId != null) {
            session.setAttribute("originalAdminId", originalAdminId);
        }
        return session;
    }
}
