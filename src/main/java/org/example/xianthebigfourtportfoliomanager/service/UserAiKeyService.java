package org.example.xianthebigfourtportfoliomanager.service;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Sylvie update: add user own api_key from settings
 */
@Service
public class UserAiKeyService {

    public static final String USER_AI_API_KEY = "USER_AI_API_KEY";

    // 个人 API Key 仅存储在当前会话，不写入数据库或配置文件。
    public void saveUserApiKey(HttpSession session, String apiKey) {
        session.setAttribute(USER_AI_API_KEY, apiKey.trim());
    }

    public void clearUserApiKey(HttpSession session) {
        session.removeAttribute(USER_AI_API_KEY);
    }

    public String getUserApiKey(HttpSession session) {
        Object raw = session.getAttribute(USER_AI_API_KEY);
        return raw instanceof String ? (String) raw : null;
    }

    public boolean isUserApiKeyConfigured(HttpSession session) {
        return StringUtils.hasText(getUserApiKey(session));
    }
}

