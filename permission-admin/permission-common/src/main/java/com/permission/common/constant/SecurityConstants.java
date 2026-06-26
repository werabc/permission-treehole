package com.permission.common.constant;

public interface SecurityConstants {

    String TOKEN_PREFIX = "Bearer ";
    String TOKEN_HEADER = "Authorization";
    String TOKEN_CACHE_PREFIX = "token:";
    String TOKEN_BLACKLIST_PREFIX = "blacklist:";
    String CAPTCHA_PREFIX = "captcha:";
    String LOGIN_FAIL_PREFIX = "login_fail:";
    String LOGIN_RATE_LIMIT_PREFIX = "rate_limit:login:";
    String LOGIN_URL = "/api/auth/login";
    String REFRESH_TOKEN_URL = "/api/auth/refresh";
    String LOGOUT_URL = "/api/auth/logout";
    String CAPTCHA_URL = "/api/auth/captcha";
    String JWT_SECRET = "permission-admin-secret-key-2024-must-be-long-enough-for-hs256";
    long TOKEN_EXPIRE = 7200;
    long REFRESH_TOKEN_EXPIRE = 604800;
    int MAX_LOGIN_FAIL_COUNT = 5;
    int ACCOUNT_LOCK_MINUTES = 30;
    int LOGIN_RATE_LIMIT_MAX = 5;
    int LOGIN_RATE_LIMIT_WINDOW = 60;
}
