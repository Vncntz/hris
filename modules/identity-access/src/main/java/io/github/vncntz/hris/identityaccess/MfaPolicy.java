package io.github.vncntz.hris.identityaccess;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Bounded operational policy; interoperable algorithm/digits/step remain fixed. */
@Component
record MfaPolicy(int skewSteps, Duration enrollmentWindow, int recoveryCount) {
    MfaPolicy(@Value("${hris.security.mfa-skew-steps:1}") int skewSteps,
            @Value("${hris.security.mfa-enrollment-window:PT10M}") Duration enrollmentWindow,
            @Value("${hris.security.mfa-recovery-count:10}") int recoveryCount) {
        if (skewSteps < 0 || skewSteps > 1 || enrollmentWindow == null || enrollmentWindow.isNegative()
                || enrollmentWindow.isZero() || enrollmentWindow.compareTo(Duration.ofMinutes(10)) > 0
                || recoveryCount < 2 || recoveryCount > 10) { throw new MfaException(); }
        this.skewSteps = skewSteps; this.enrollmentWindow = enrollmentWindow; this.recoveryCount = recoveryCount;
    }
}
