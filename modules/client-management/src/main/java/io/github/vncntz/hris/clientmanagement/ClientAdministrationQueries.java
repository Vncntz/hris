package io.github.vncntz.hris.clientmanagement;

import java.util.UUID;

/** Authorized Client master browsing. Offsets are zero-based; limits are 1 through MAX_LIMIT.
 * Pages use display-name ascending database collation order, then public UUID ascending.
 * Each call observes committed state independently; concurrent edits may move rows between calls.
 */
public interface ClientAdministrationQueries {
    int MAX_LIMIT = 200;
    ClientCompanyPage companies(int offset, int limit);
    /** Null parents are invalid; unknown parents produce NOT_FOUND, including at empty offsets. */
    ClientSitePage sites(UUID companyPublicId, int offset, int limit);
}
