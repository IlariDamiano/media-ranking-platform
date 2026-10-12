
package com.ilaridamiano.backend.integration.sync.repository;

import com.ilaridamiano.backend.integration.sync.entity.ProviderSyncState;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import com.ilaridamiano.backend.integration.sync.entity.ProviderSyncState;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class ProviderSyncLeaseRepositoryIntegrationTest {

    @Autowired
    private ProviderSyncStateRepository repository;

    @Test
    @Transactional
    void preventsSecondOwnerFromAcquiringActiveLease() {

        String provider = "TEST_SYNC_LEASE_EXCLUSIVE";

        UUID firstOwner = UUID.randomUUID();
        UUID secondOwner = UUID.randomUUID();

        Instant now = Instant.parse("2026-10-12T09:00:00Z");
        Instant leaseUntil = now.plus(Duration.ofMinutes(10));

        // Job A attempts to acquire the lease.
        int firstResult = repository.tryAcquireSyncLease(
                provider,
                firstOwner,
                leaseUntil,
                now
        );

        // Job B attempts to acquire the same lease.
        int secondResult = repository.tryAcquireSyncLease(
                provider,
                secondOwner,
                leaseUntil,
                now
        );

        // Only the first acquisition should succeed.
        assertThat(firstResult).isEqualTo(1);
        assertThat(secondResult).isZero();

        // PostgreSQL must preserve Job A's ownership.
        ProviderSyncState state = repository.findById(provider)
                .orElseThrow();

        assertThat(state.getSyncLeaseOwner())
                .isEqualTo(firstOwner);

        assertThat(state.getSyncLeaseUntil())
                .isEqualTo(leaseUntil);
    }

    @Test
    @Transactional
    void preventsLeaseAcquisitionDuringProviderCooldown() {

        String provider = "TEST_SYNC_LEASE_COOLDOWN";

        UUID owner = UUID.randomUUID();

        Instant now = Instant.parse("2026-10-12T09:00:00Z");

        // TMDB is cooling down for another 30 minutes.
        ProviderSyncState state = new ProviderSyncState(provider);
        state.setCooldownUntil(now.plus(Duration.ofMinutes(30)));

        repository.saveAndFlush(state);

        // A synchronization job tries to acquire a 10-minute lease.
        int result = repository.tryAcquireSyncLease(
                provider,
                owner,
                now.plus(Duration.ofMinutes(10)),
                now
        );

        // The acquisition must be rejected.
        assertThat(result).isZero();

        // The provider must not have a lease owner.
        ProviderSyncState storedState = repository.findById(provider)
                .orElseThrow();

        assertThat(storedState.getSyncLeaseOwner()).isNull();
        assertThat(storedState.getSyncLeaseUntil()).isNull();
    }

    @Test
    @Transactional
    void allowsNewOwnerToReplaceExpiredLease() {

        String provider = "TEST_SYNC_LEASE_EXPIRED";

        UUID firstOwner = UUID.randomUUID();
        UUID secondOwner = UUID.randomUUID();

        Instant initialTime = Instant.parse("2026-10-12T09:00:00Z");

        Instant firstLeaseUntil = initialTime.plus(
                Duration.ofMinutes(10)
        );

        // Job A acquires the initial lease.
        int firstResult = repository.tryAcquireSyncLease(
                provider,
                firstOwner,
                firstLeaseUntil,
                initialTime
        );

        assertThat(firstResult).isEqualTo(1);

        // Simulate 11 minutes passing.
        Instant laterTime = initialTime.plus(
                Duration.ofMinutes(11)
        );

        Instant secondLeaseUntil = laterTime.plus(
                Duration.ofMinutes(10)
        );

        // Job B attempts to replace the expired lease.
        int secondResult = repository.tryAcquireSyncLease(
                provider,
                secondOwner,
                secondLeaseUntil,
                laterTime
        );

        assertThat(secondResult).isEqualTo(1);

        // Verify that Job B is now the lease owner.
        ProviderSyncState storedState = repository.findById(provider)
                .orElseThrow();

        assertThat(storedState.getSyncLeaseOwner())
                .isEqualTo(secondOwner);

        assertThat(storedState.getSyncLeaseUntil())
                .isEqualTo(secondLeaseUntil);
    }

    @Test
    @Transactional
    void onlyCurrentOwnerCanRenewActiveLease() {

        String provider = "TEST_SYNC_LEASE_RENEWAL";
        UUID owner = UUID.randomUUID();
        UUID otherOwner = UUID.randomUUID();

        Instant now = Instant.parse("2026-10-12T09:00:00Z");

        assertThat(repository.tryAcquireSyncLease(
                provider,
                owner,
                now.plus(Duration.ofMinutes(10)),
                now
        )).isEqualTo(1);

        // Another owner cannot renew this lease.
        assertThat(repository.renewSyncLease(
                provider,
                otherOwner,
                now.plus(Duration.ofMinutes(30)),
                now.plus(Duration.ofMinutes(1))
        )).isZero();

        // The current owner can renew it.
        assertThat(repository.renewSyncLease(
                provider,
                owner,
                now.plus(Duration.ofMinutes(30)),
                now.plus(Duration.ofMinutes(1))
        )).isEqualTo(1);

        // Once expired, even the previous owner cannot renew.
        assertThat(repository.renewSyncLease(
                provider,
                owner,
                now.plus(Duration.ofMinutes(60)),
                now.plus(Duration.ofMinutes(31))
        )).isZero();

        ProviderSyncState state =
                repository.findById(provider).orElseThrow();

        assertThat(state.getSyncLeaseOwner()).isEqualTo(owner);
        assertThat(state.getSyncLeaseUntil())
                .isEqualTo(now.plus(Duration.ofMinutes(30)));
    }

    @Test
    @Transactional
    void previousOwnerCannotReleaseReplacedLease() {

        String provider = "TEST_LEASE_SAFE_RELEASE";

        UUID firstOwner = UUID.randomUUID();
        UUID secondOwner = UUID.randomUUID();

        Instant now = Instant.parse("2026-10-12T09:00:00Z");
        Instant later = now.plus(Duration.ofMinutes(11));

        assertThat(repository.tryAcquireSyncLease(
                provider,
                firstOwner,
                now.plus(Duration.ofMinutes(10)),
                now
        )).isEqualTo(1);

        // First lease expires; second owner takes over.
        assertThat(repository.tryAcquireSyncLease(
                provider,
                secondOwner,
                later.plus(Duration.ofMinutes(30)),
                later
        )).isEqualTo(1);

        // Previous owner must not release the new lease.
        assertThat(repository.releaseSyncLease(
                provider,
                firstOwner,
                later
        )).isZero();

        // Current owner can release it.
        assertThat(repository.releaseSyncLease(
                provider,
                secondOwner,
                later
        )).isEqualTo(1);

        ProviderSyncState state =
                repository.findById(provider).orElseThrow();

        assertThat(state.getSyncLeaseOwner()).isNull();
        assertThat(state.getSyncLeaseUntil()).isNull();
    }

}
