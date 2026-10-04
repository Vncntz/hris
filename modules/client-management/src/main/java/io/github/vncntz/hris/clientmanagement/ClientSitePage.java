package io.github.vncntz.hris.clientmanagement;

import java.util.List;

/** Immutable bounded administration rows; hasMore indicates a row beyond this page. */
public record ClientSitePage(List<ClientSiteReference> rows, boolean hasMore) {
    public ClientSitePage { rows = List.copyOf(rows); }
}
