package io.github.vncntz.hris.identityaccess;

import java.util.UUID;

/** Application-facing boundary for future service authorization checks. */
public interface CurrentActor {
    UUID requireUserId();

    boolean hasAuthority(String authority);

    void requireAuthority(String authority);
}
