
package com.sneakerhouse.backend.service;

import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionIdListener;
import jakarta.servlet.http.HttpSessionListener;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class AccountSessionRegistry
        implements HttpSessionListener, HttpSessionIdListener {

    private final ConcurrentMap<String, TrackedSession> sessions =
            new ConcurrentHashMap<>();

    public record SessionView(
            String id,
            String device,
            String browser,
            String os,
            Instant createdAt,
            Instant lastAccessedAt,
            boolean current,
            String status
    ) {}

    private record TrackedSession(
            String publicId,
            String httpSessionId,
            HttpSession httpSession,
            String email,
            Instant loggedInAt,
            String device,
            String browser,
            String os
    ) {}

    // Dang ky phien sau khi dang nhap thanh cong
    public void register(
            HttpSession session,
            String email,
            String userAgent
    ) {
        if (session == null || email == null || email.isBlank()) {
            throw new IllegalArgumentException(
                    "Authenticated session and email are required"
            );
        }

        String httpId = session.getId();
        removeByHttpId(httpId);

        String agent = userAgent == null
                ? ""
                : userAgent.toLowerCase(Locale.ROOT);

        String publicId = UUID.randomUUID().toString();

        sessions.put(publicId, new TrackedSession(
                publicId,
                httpId,
                session,
                normalize(email),
                Instant.now(),
                detectDevice(agent),
                detectBrowser(agent),
                detectOs(agent)
        ));
    }

    // Lay danh sach phien cua tai khoan
    public List<SessionView> list(
            String email,
            HttpSession currentSession
    ) {
        String owner = normalize(email);

        String currentId = currentSession == null
                ? null
                : currentSession.getId();

        return sessions.values().stream()
                .filter(s -> s.email().equals(owner))
                .filter(this::isActive)
                .map(s -> new SessionView(
                        s.publicId(),
                        s.device(),
                        s.browser(),
                        s.os(),
                        s.loggedInAt(),
                        Instant.ofEpochMilli(
                                s.httpSession().getLastAccessedTime()
                        ),
                        s.httpSessionId().equals(currentId),
                        "ACTIVE"
                ))
                .sorted(
                        Comparator.comparing(
                                SessionView::createdAt
                        ).reversed()
                )
                .toList();
    }

    // Thu hoi phien khac cua chinh tai khoan
    public void revoke(
            String ownerEmail,
            HttpSession currentSession,
            String publicId
    ) {
        TrackedSession target = sessions.get(publicId);

        if (target == null
                || !target.email().equals(normalize(ownerEmail))
                || !isActive(target)) {

            throw new AuthException(
                    404,
                    "SESSION_NOT_FOUND",
                    "Không tìm thấy phiên đăng nhập."
            );
        }

        if (currentSession != null
                && target.httpSessionId().equals(
                        currentSession.getId()
                )) {

            throw new AuthException(
                    409,
                    "CURRENT_SESSION",
                    "Không thể thu hồi phiên hiện tại."
            );
        }

        try {
            // Huy phien tren server, khong chi xoa khoi giao dien
            target.httpSession().invalidate();

        } catch (IllegalStateException e) {

            sessions.remove(publicId, target);

            throw new AuthException(
                    404,
                    "SESSION_NOT_FOUND",
                    "Phiên đăng nhập không còn hoạt động."
            );
        }

        sessions.remove(publicId, target);
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent event) {
        removeByHttpId(event.getSession().getId());
    }

    @Override
    public void sessionIdChanged(
            HttpSessionEvent event,
            String oldSessionId
    ) {
        removeByHttpId(oldSessionId);
    }

    private void removeByHttpId(String httpSessionId) {
        sessions.forEach((id, item) -> {
            if (item.httpSessionId().equals(httpSessionId)) {
                sessions.remove(id, item);
            }
        });
    }

    private boolean isActive(TrackedSession item) {
        try {
            item.httpSession().getLastAccessedTime();
            return true;

        } catch (IllegalStateException e) {
            sessions.remove(item.publicId(), item);
            return false;
        }
    }

    private static String normalize(String email) {
        return email == null
                ? ""
                : email.trim().toLowerCase(Locale.ROOT);
    }

    private static String detectDevice(String ua) {
        if (ua.contains("ipad") || ua.contains("tablet")) {
            return "Tablet";
        }

        if (ua.contains("mobile")
                || ua.contains("iphone")
                || ua.contains("android")) {
            return "Mobile";
        }

        return "Desktop";
    }

    private static String detectBrowser(String ua) {
        if (ua.contains("edg/") || ua.contains("edge/")) {
            return "Microsoft Edge";
        }

        if (ua.contains("firefox/")) {
            return "Firefox";
        }

        if (ua.contains("chrome/") || ua.contains("crios/")) {
            return "Chrome";
        }

        if (ua.contains("safari/")) {
            return "Safari";
        }

        return "Unknown";
    }

    private static String detectOs(String ua) {
        if (ua.contains("windows")) return "Windows";
        if (ua.contains("android")) return "Android";

        if (ua.contains("iphone")
                || ua.contains("ipad")
                || ua.contains("ios")) {
            return "iOS";
        }

        if (ua.contains("mac os")) return "macOS";
        if (ua.contains("linux")) return "Linux";

        return "Unknown";
    }
}
