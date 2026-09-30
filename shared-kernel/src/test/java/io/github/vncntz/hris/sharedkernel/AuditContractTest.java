package io.github.vncntz.hris.sharedkernel;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AuditContractTest {
    @Test
    void acceptsMinimalAndBoundedRequests() {
        AuditRequest request = new AuditRequest(" operator:7 ", "APPROVED", "request", "r-14", null,
                "synthetic workflow summary");
        assertEquals("operator:7", request.actorReference());
        assertNull(request.reason());
        assertEquals("synthetic workflow summary", request.context());

        new AuditRequest("a".repeat(128), "a".repeat(64), "t".repeat(64),
                "r".repeat(128), "x".repeat(512), "c".repeat(1024));
    }

    @Test
    void rejectsMissingBlankOversizedAndControlText() {
        assertThrows(IllegalArgumentException.class,
                () -> new AuditRequest(null, "APPROVED", "request", "r-14", null, null));
        assertThrows(IllegalArgumentException.class,
                () -> new AuditRequest("a", "  ", "request", "r-14", null, null));
        assertThrows(IllegalArgumentException.class,
                () -> new AuditRequest("a", "APPROVED", "request", "r".repeat(129), null, null));
        assertThrows(IllegalArgumentException.class,
                () -> new AuditRequest("a", "APPROVED", "request", "r-14", " ", null));
        assertThrows(IllegalArgumentException.class,
                () -> new AuditRequest("a", "APPROVED", "request", "r-14", null, "line\nfeed"));
        assertThrows(IllegalArgumentException.class,
                () -> new AuditRequest("a", "APPROVED", "request", "r-14", null,
                        "c".repeat(1025)));
    }
}
