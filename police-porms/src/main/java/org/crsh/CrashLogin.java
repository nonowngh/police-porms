package org.crsh;

import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
public class CrashLogin {

    private static final String LOGIN_PROPERTY_PREFIX = "crashLogin.";

    public boolean isLogin(Long threadId) {
        String key = LOGIN_PROPERTY_PREFIX + threadId;
        String value = System.getProperty(key);
        log.info("isLogin threadId: {}, result: {}", threadId, value);
        return Boolean.parseBoolean(value);
    }

    public void setLogin(Long threadId, boolean status) {
        String key = LOGIN_PROPERTY_PREFIX + threadId;
        System.setProperty(key, Boolean.toString(status));
        log.info("setLogin threadId: {}, status: {}", threadId, status);
    }


}
