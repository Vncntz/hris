package io.github.vncntz.hris.identityaccess;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Technical coordination state; never tied to the lifetime of an account. */
@Entity
@Table(name = "identity_bootstrap_state")
class BootstrapStateEntity {
    @Id
    private Integer id;

    @Column(nullable = false)
    private boolean completed;

    protected BootstrapStateEntity() {
    }

    boolean completed() {
        return completed;
    }

    void complete() {
        completed = true;
    }
}
