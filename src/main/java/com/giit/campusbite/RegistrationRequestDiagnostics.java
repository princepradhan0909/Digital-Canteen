package com.giit.campusbite;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.IOException;
import java.util.UUID;

@Configuration
class RegistrationRequestDiagnostics implements WebMvcConfigurer {

    private static final String REQUEST_ID_ATTRIBUTE =
            RegistrationRequestDiagnostics.class.getName() + ".requestId";
    private static final String REGISTRATION_PATH = "/api/auth/register";
    private static final Logger CONTROLLER_LOGGER =
            LoggerFactory.getLogger("campusbite.registration-controller-diagnostics");

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        HandlerInterceptor interceptor = new HandlerInterceptor() {
            @Override
            public void afterCompletion(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Object handler,
                                        Exception exception) {
                Object requestId = request.getAttribute(REQUEST_ID_ATTRIBUTE);
                if (requestId instanceof String id) {
                    CONTROLLER_LOGGER.info("requestId={} method={} path={} status={}",
                            id, request.getMethod(), request.getServletPath(),
                            response.getStatus());
                }
            }
        };
        registry.addInterceptor(interceptor).addPathPatterns(REGISTRATION_PATH);
    }

    static final class Filter extends OncePerRequestFilter {

        private static final Logger FILTER_LOGGER =
                LoggerFactory.getLogger("campusbite.registration-filter-diagnostics");

        @Override
        protected boolean shouldNotFilter(HttpServletRequest request) {
            return !"POST".equals(request.getMethod())
                    || !REGISTRATION_PATH.equals(request.getServletPath());
        }

        @Override
        protected void doFilterInternal(HttpServletRequest request,
                                        HttpServletResponse response,
                                        FilterChain filterChain)
                throws ServletException, IOException {
            String requestId = UUID.randomUUID().toString();
            request.setAttribute(REQUEST_ID_ATTRIBUTE, requestId);
            response.setHeader("X-Request-ID", requestId);
            try {
                filterChain.doFilter(request, response);
            } finally {
                FILTER_LOGGER.info("requestId={} method={} path={} status={}",
                        requestId, request.getMethod(), request.getServletPath(),
                        response.getStatus());
            }
        }
    }
}
