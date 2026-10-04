package io.github.vncntz.hris.clientmanagement;

import java.util.List;

/** Immutable bounded administration rows; hasMore indicates a row beyond this page. */
public record ClientCompanyPage(List<ClientCompanyReference> rows, boolean hasMore) {
    public ClientCompanyPage { rows = List.copyOf(rows); }
}
