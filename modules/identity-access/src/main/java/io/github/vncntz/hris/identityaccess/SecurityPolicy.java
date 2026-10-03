package io.github.vncntz.hris.identityaccess;

import java.time.Duration;

record SecurityPolicy(int maxFailedAttempts, Duration lockDuration, Duration reauthenticationWindow) {
    SecurityPolicy(int maxFailedAttempts, Duration lockDuration) {
        this(maxFailedAttempts, lockDuration, Duration.ofMinutes(5));
    }

    SecurityPolicy {
        if (reauthenticationWindow == null || reauthenticationWindow.isNegative()
                || reauthenticationWindow.isZero() || reauthenticationWindow.compareTo(Duration.ofMinutes(30)) > 0) {
            throw new IllegalArgumentException("Invalid recent authentication window");
        }
        if (maxFailedAttempts < 1 || maxFailedAttempts > 100) {
            throw new IllegalArgumentException("Invalid authentication failure limit");
        }
        if (lockDuration.isNegative() || lockDuration.isZero()
                || lockDuration.compareTo(Duration.ofDays(1)) > 0) {
            throw new IllegalArgumentException("Invalid authentication lock duration");
        }
    }
}
