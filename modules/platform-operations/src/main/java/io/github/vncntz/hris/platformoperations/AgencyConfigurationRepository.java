package io.github.vncntz.hris.platformoperations;

import org.springframework.data.jpa.repository.JpaRepository;

/** Internal persistence only; callers use AgencyConfigurationService. */
interface AgencyConfigurationRepository extends JpaRepository<AgencyConfigurationEntity, Byte> {
}
