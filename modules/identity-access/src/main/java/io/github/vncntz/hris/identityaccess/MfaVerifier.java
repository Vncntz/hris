package io.github.vncntz.hris.identityaccess;

import java.time.Instant;
import java.util.Arrays;
import org.springframework.stereotype.Component;

/** Must be called under the Account write lock; consumption commits with authentication. */
@Component
class MfaVerifier {
    private final MfaSecrets secrets;
    private final MfaPolicy policy;
    MfaVerifier(MfaSecrets secrets, MfaPolicy policy) { this.secrets = secrets; this.policy = policy; }
    boolean verify(AccountEntity account, char[] supplied, Instant now) {
        if (!account.mfaEnabled()) { return true; }
        // Even recovery authentication requires a usable installation key.
        byte[] seed = secrets.decrypt(account.publicId(), account.mfaSecret());
        try {
            if (supplied != null && supplied.length == 32) {
                return account.consumeRecovery(Totp.recoveryDigest(supplied));
            }
            long step = Totp.match(seed, supplied, now, account.mfaLastStep(), policy.skewSteps());
            if (step < 0) { return false; }
            account.consumeStep(step);
            return true;
        } finally { Arrays.fill(seed, (byte) 0); }
    }
}
