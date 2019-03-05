package com.wk.middleware.util;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;

import javax.servlet.http.HttpServletResponse;

/**
 * Created by jince on 2018/11/27.
 */
public final class WebUtil {

    private WebUtil() {
    }

    public static String getCurrentUsername() {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {

            Object o = authentication.getPrincipal();

            if (o != null && o instanceof User) {
                return ((User) o).getUsername();
            }
        }

        return null;
    }

    public static void disableCORS(HttpServletResponse response) {
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Access-Control-Allow-Methods", "POST, GET, OPTIONS, DELETE");
        response.setHeader("Access-Control-Max-Age", "3600");
        response.setHeader("Access-Control-Allow-Headers", "ALL");
        response.setHeader("Access-Control-Allow-Credentials", "true");
    }
}
