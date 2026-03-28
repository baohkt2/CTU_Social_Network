package com.ctuconnect.security.aspect;

import com.ctuconnect.security.AuthenticatedUser;
import com.ctuconnect.security.SecurityContextHolder;
import com.ctuconnect.security.annotation.RequireAuth;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Arrays;

@Slf4j
@Aspect
@Component
public class AuthorizationAspect {

    @Around("@within(com.ctuconnect.security.annotation.RequireAuth) || @annotation(com.ctuconnect.security.annotation.RequireAuth)")
    public Object authorize(ProceedingJoinPoint joinPoint) throws Throwable {
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();

        // Ưu tiên annotation ở method, nếu không có thì lấy ở class
        RequireAuth annotation = method.getAnnotation(RequireAuth.class);
        if (annotation == null) {
            annotation = joinPoint.getTarget().getClass().getAnnotation(RequireAuth.class);
        }

        if (annotation == null) {
            return joinPoint.proceed(); // Không có yêu cầu quyền → cho qua
        }

        AuthenticatedUser currentUser = SecurityContextHolder.getAuthenticatedUser();

        if (currentUser == null) {
            log.warn("Blocked access: no authenticated user");
            throw new SecurityException("Unauthorized: No authenticated user");
        }

        // Kiểm tra roles nếu được chỉ định
        String[] requiredRoles = annotation.roles();
        if (requiredRoles.length > 0) {
            boolean match = Arrays.stream(requiredRoles)
                    .anyMatch(currentUser::hasRole);
            if (!match) {
                log.warn("Blocked access: user {} does not have required role(s): {}", currentUser.getUserId(), Arrays.toString(requiredRoles));
                throw new SecurityException("Forbidden: Insufficient role");
            }
        }

        // Kiểm tra quyền selfOnly nếu có
        if (annotation.selfOnly()) {
            String currentUserId = currentUser.getUserId();
            String userIdInArgs = findUserIdFromArguments(joinPoint.getArgs());

            if (userIdInArgs != null && !userIdInArgs.equals(currentUserId)) {
                log.warn("Blocked access: selfOnly=true, but userId {} != {}", currentUserId, userIdInArgs);
                throw new SecurityException("Forbidden: You can only access your own data");
            }
        }

        return joinPoint.proceed();
    }

    /**
     * Trích xuất userId từ tham số method nếu có
     * - Ưu tiên tìm argument có tên 'userId' hoặc UUID-like string
     */
    @Nullable
    private String findUserIdFromArguments(@Nonnull Object[] args) {
        for (Object arg : args) {
            if (arg instanceof String str) {
                if (isUuid(str)) return str;
            }
        }
        return null;
    }

    private boolean isUuid(String s) {
        return s != null && s.matches("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[1-5][0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$");
    }
}
