package io.github.vncntz.hris.identityaccess;

import java.util.Map;
import java.util.UUID;

/** Non-secret transient public Role UUID/generation proof; never persisted in a session. */
public record RoleGenerations(Map<UUID, Long> values) {
    public RoleGenerations {
        values = Map.copyOf(values);
        if (values.values().stream().anyMatch(value -> value < 0)) {
            throw new IllegalArgumentException("Invalid role generation");
        }
    }
}
