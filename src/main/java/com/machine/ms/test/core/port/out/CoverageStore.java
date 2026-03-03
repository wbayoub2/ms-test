package com.machine.ms.test.core.port.out;

import com.machine.ms.test.core.domain.model.CoverageSnapshot;

import java.util.List;
import java.util.Optional;

public interface CoverageStore {
    CoverageSnapshot save(CoverageSnapshot snapshot);

    Optional<CoverageSnapshot> findByCommitSha(String commitSha);

    List<CoverageSnapshot> findByServiceAndBranch(String service, String branch);

    List<CoverageSnapshot> findAll();
}
