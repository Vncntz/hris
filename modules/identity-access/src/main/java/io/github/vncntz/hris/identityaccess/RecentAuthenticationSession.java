package io.github.vncntz.hris.identityaccess;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

/** Local session port. An attempt discards earlier proof and publishes only after success. */
public interface RecentAuthenticationSession {
    record Proof(UUID accountId, Instant provenAt) {
        public Proof {
            java.util.Objects.requireNonNull(accountId);
            java.util.Objects.requireNonNull(provenAt);
        }
        @Override public String toString() { return "Recent authentication proof"; }
    }

    /** Serialize attempts in one session; failed verification/publication leaves it unproven. */
    void attempt(Supplier<Proof> verification);

    /** Empty for absent, invalid or revoked sessions. Never creates a session. */
    Optional<Proof> proof();
}
