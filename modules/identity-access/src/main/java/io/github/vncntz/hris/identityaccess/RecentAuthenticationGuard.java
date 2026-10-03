package io.github.vncntz.hris.identityaccess;

import java.time.Clock;
import java.time.Duration;
import org.springframework.stereotype.Component;

/** Credential freshness is independent of authority; callers authorize before invoking it. */
@Component
public class RecentAuthenticationGuard {
    private final CurrentActor actor;
    private final RecentAuthenticationSession session;
    private final SecurityPolicy policy;
    private final Clock clock;

    RecentAuthenticationGuard(CurrentActor actor, RecentAuthenticationSession session,
            SecurityPolicy policy, Clock clock) {
        this.actor = actor;
        this.session = session;
        this.policy = policy;
        this.clock = clock;
    }

    public void requireRecentAuthentication() {
        var id = actor.requireUserId();
        var proof = session.proof().orElseThrow(() ->
                new RecentAuthenticationException(RecentAuthenticationException.Reason.PROOF_REQUIRED));
        var now = clock.instant();
        if (!id.equals(proof.accountId()) || proof.provenAt().isAfter(now)
                || Duration.between(proof.provenAt(), now).compareTo(policy.reauthenticationWindow()) >= 0) {
            throw new RecentAuthenticationException(RecentAuthenticationException.Reason.PROOF_REQUIRED);
        }
    }
}
