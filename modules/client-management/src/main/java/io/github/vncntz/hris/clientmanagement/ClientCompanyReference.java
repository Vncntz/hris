package io.github.vncntz.hris.clientmanagement;

import java.util.UUID;

/** Immutable identity and lifecycle projection; version is an edit token, not a relational ID. */
public record ClientCompanyReference(UUID publicId, String displayName, boolean active, long version) {}
