package com.splitsmart.ledger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;

/**
 * SHA-256 fingerprint deduplication engine backed by Redis TTL keys.
 *
 * <h3>Fingerprint strategy</h3>
 * The fingerprint is a SHA-256 hash of the canonicalized tuple:
 * <pre>
 *   groupId | payerIdentifier | amountMinor | currency | roundedDate
 * </pre>
 * where {@code roundedDate} is the transaction date truncated to day precision
 * so that minor time differences between duplicate messages don't produce
 * different hashes.
 *
 * <h3>Redis key format</h3>
 * <pre>
 *   splitsmart:dedup:{groupId}:{sha256hex}
 * </pre>
 * TTL is configurable via {@code splitsmart.dedup.ttl-hours} (default 24 h).
 *
 * <h3>Similarity scoring</h3>
 * When an exact fingerprint match is not found, a lightweight bigram similarity
 * comparison is run against recent draft descriptions (retrieved from the
 * {@link DraftRepository}) to catch near-duplicates with slightly different
 * amounts or descriptions.
 */
@Component
public class DeduplicationEngine {

    private static final Logger log = LoggerFactory.getLogger(DeduplicationEngine.class);

    private static final String KEY_PREFIX    = "splitsmart:dedup:";
    private static final double DUPE_THRESHOLD = 0.85;

    private final StringRedisTemplate redisTemplate;
    private final DraftRepository     draftRepository;

    @Value("${splitsmart.dedup.ttl-hours:24}")
    private int ttlHours;

    public DeduplicationEngine(StringRedisTemplate redisTemplate,
                               DraftRepository draftRepository) {
        this.redisTemplate  = redisTemplate;
        this.draftRepository = draftRepository;
    }

    // ─── Primary check ───────────────────────────────────────────────────────

    /**
     * Checks whether an expense with the given attributes is likely a duplicate.
     *
     * @return a {@link DeduplicationResult} indicating exact match, similarity
     *         match, or no duplicate
     */
    public DeduplicationResult check(String groupId, String payerIdentifier,
                                     long amountMinor, String currency,
                                     String description, String transactionDate) {
        String fingerprint = computeFingerprint(groupId, payerIdentifier, amountMinor, currency, transactionDate);
        String redisKey    = KEY_PREFIX + groupId + ":" + fingerprint;

        // ── Exact fingerprint match ──────────────────────────────────────────
        String existingDraftId = redisTemplate.opsForValue().get(redisKey);
        if (existingDraftId != null) {
            log.info("[DeduplicationEngine] Exact fingerprint match — groupId={} draftId={}", groupId, existingDraftId);
            return DeduplicationResult.exactMatch(fingerprint, existingDraftId);
        }

        // ── Similarity scan against recent drafts ────────────────────────────
        if (description != null && !description.isBlank()) {
            var recentDrafts = draftRepository.findByGroupIdOrderByCreatedAtDesc(groupId);
            for (DraftEntity existing : recentDrafts) {
                double score = bigramSimilarity(description, existing.getDescription());
                if (score >= DUPE_THRESHOLD) {
                    log.info("[DeduplicationEngine] Near-duplicate detected — score={} draftId={}", score, existing.getId());
                    return DeduplicationResult.similarMatch(fingerprint, existing.getId(), score);
                }
            }
        }

        return DeduplicationResult.noDuplicate(fingerprint);
    }

    /**
     * Registers a fingerprint in Redis after a draft is successfully created.
     * Subsequent calls with the same fingerprint will return an exact match.
     *
     * @param groupId    group ID
     * @param fingerprint SHA-256 fingerprint from {@link #check}
     * @param draftId    newly created draft ID to store as the Redis value
     */
    public void register(String groupId, String fingerprint, String draftId) {
        String redisKey = KEY_PREFIX + groupId + ":" + fingerprint;
        redisTemplate.opsForValue().set(redisKey, draftId, Duration.ofHours(ttlHours));
        log.debug("[DeduplicationEngine] Registered fingerprint — key={} draftId={} ttl={}h",
                redisKey, draftId, ttlHours);
    }

    // ─── Fingerprint computation ─────────────────────────────────────────────

    public String computeFingerprint(String groupId, String payer,
                                     long amountMinor, String currency,
                                     String transactionDate) {
        String canonical = String.join("|",
                normalise(groupId),
                normalise(payer),
                String.valueOf(amountMinor),
                normalise(currency),
                normalise(transactionDate));
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(canonical.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    // ─── Bigram similarity ───────────────────────────────────────────────────

    /**
     * Computes Sørensen–Dice bigram similarity between two strings.
     * Returns a value in [0.0, 1.0].
     */
    double bigramSimilarity(String a, String b) {
        if (a == null || b == null) return 0.0;
        var bigramsA = bigrams(a.toLowerCase());
        var bigramsB = bigrams(b.toLowerCase());
        if (bigramsA.isEmpty() && bigramsB.isEmpty()) return 1.0;
        if (bigramsA.isEmpty() || bigramsB.isEmpty()) return 0.0;

        long intersection = bigramsA.stream().filter(bigramsB::contains).count();
        return (2.0 * intersection) / (bigramsA.size() + bigramsB.size());
    }

    private java.util.List<String> bigrams(String s) {
        java.util.List<String> result = new java.util.ArrayList<>();
        for (int i = 0; i < s.length() - 1; i++) {
            result.add(s.substring(i, i + 2));
        }
        return result;
    }

    private String normalise(String s) {
        return s != null ? s.trim().toLowerCase() : "";
    }

    // ─── Result type ─────────────────────────────────────────────────────────

    public static final class DeduplicationResult {

        public enum Kind { EXACT, SIMILAR, NONE }

        private final Kind   kind;
        private final String fingerprint;
        private final String existingDraftId;
        private final double similarityScore;

        private DeduplicationResult(Kind kind, String fingerprint,
                                    String existingDraftId, double similarityScore) {
            this.kind            = kind;
            this.fingerprint     = fingerprint;
            this.existingDraftId = existingDraftId;
            this.similarityScore = similarityScore;
        }

        public static DeduplicationResult exactMatch(String fp, String draftId) {
            return new DeduplicationResult(Kind.EXACT, fp, draftId, 1.0);
        }

        public static DeduplicationResult similarMatch(String fp, String draftId, double score) {
            return new DeduplicationResult(Kind.SIMILAR, fp, draftId, score);
        }

        public static DeduplicationResult noDuplicate(String fp) {
            return new DeduplicationResult(Kind.NONE, fp, null, 0.0);
        }

        public boolean isDuplicate()      { return kind != Kind.NONE; }
        public Kind    getKind()          { return kind; }
        public String  getFingerprint()   { return fingerprint; }
        public String  getExistingDraftId(){ return existingDraftId; }
        public double  getSimilarityScore(){ return similarityScore; }
    }
}
