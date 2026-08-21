package com.splitsmart.ledger;

/**
 * Thrown by the CQRS write path when an Optimistic Concurrency Control (OCC)
 * version conflict is detected on an {@link EventEntity} append operation.
 *
 * <p>The ledger uses an append-only event store where each event carries a
 * monotonically increasing {@code version} per group. When two concurrent
 * writers attempt to append at the same version, the second writer receives
 * this exception and must reload the latest state before retrying.
 *
 * <p>Callers are expected to handle this exception by:
 * <ol>
 *   <li>Reading the current head version from the event store.</li>
 *   <li>Re-validating the business rules against the updated state.</li>
 *   <li>Retrying the append with the new version number.</li>
 * </ol>
 *
 * <p>This exception is intentionally unchecked so that it propagates cleanly
 * through the Spring transaction boundary without requiring try-catch boilerplate
 * at every call site.
 */
public class OptimisticLockingException extends RuntimeException {

    /** Group ID in whose event stream the conflict occurred. */
    private final String groupId;

    /** Version number that was expected by the writer. */
    private final long expectedVersion;

    /** Actual version found in the store at the time of the conflict. */
    private final long actualVersion;

    public OptimisticLockingException(String groupId, long expectedVersion, long actualVersion) {
        super(String.format(
                "OCC conflict for group '%s': expected version %d but found %d",
                groupId, expectedVersion, actualVersion));
        this.groupId         = groupId;
        this.expectedVersion = expectedVersion;
        this.actualVersion   = actualVersion;
    }

    public OptimisticLockingException(String message) {
        super(message);
        this.groupId         = null;
        this.expectedVersion = -1;
        this.actualVersion   = -1;
    }

    public String getGroupId()         { return groupId; }
    public long getExpectedVersion()   { return expectedVersion; }
    public long getActualVersion()     { return actualVersion; }
}
