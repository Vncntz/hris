package io.github.vncntz.hris.identityaccess;

import java.time.Duration;

record SecurityPolicy(int maxFailedAttempts, Duration lockDuration) {
    SecurityPolicy {
        if (maxFailedAttempts < 1 || maxFailedAttempts > 100) {
            throw new IllegalArgumentException("Invalid authentication failure limit");
        }
        if (lockDuration.isNegative() || lockDuration.isZero()
                || lockDuration.compareTo(Duration.ofDays(1)) > 0) {
            throw new IllegalArgumentException("Invalid authentication lock duration");
        }
    }
}
