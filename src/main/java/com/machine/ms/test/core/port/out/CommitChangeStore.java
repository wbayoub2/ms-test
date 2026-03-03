package com.machine.ms.test.core.port.out;

import com.machine.ms.test.core.domain.model.CommitChangeSnapshot;

import java.util.Optional;

public interface CommitChangeStore {

    CommitChangeSnapshot save(CommitChangeSnapshot snapshot);

    Optional<CommitChangeSnapshot> findByHeadCommit(String service, String headCommit);
}
