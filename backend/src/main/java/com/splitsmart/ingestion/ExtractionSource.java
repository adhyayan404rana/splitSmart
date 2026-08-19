package com.splitsmart.ingestion;

/**
 * Enumeration of the channels through which an expense description
 * can reach the SplitSmart ingestion pipeline.
 *
 * <p>The source is propagated through the full pipeline so that:
 * <ul>
 *   <li>Audit events can record provenance (e.g. "parsed from Telegram message 42").</li>
 *   <li>Confidence thresholds can be tuned per channel (structured API input is
 *       inherently more reliable than a free-form WhatsApp message).</li>
 *   <li>Response formatting can be channel-aware (Telegram supports MarkdownV2;
 *       WhatsApp uses plain text with limited templating).</li>
 * </ul>
 */
public enum ExtractionSource {

    /**
     * Structured JSON payload submitted directly to the REST API
     * (e.g. from the SplitSmart web or mobile app).
     */
    API,

    /**
     * Free-form or semi-structured text message received via the
     * Telegram Bot API webhook.
     */
    TELEGRAM,

    /**
     * Free-form text message received via the WhatsApp Cloud API
     * (Meta Business Platform) webhook.
     */
    WHATSAPP,

    /**
     * OCR-extracted text from a receipt image uploaded through
     * any channel. The raw OCR transcript is then fed through
     * the NLP pipeline.
     */
    RECEIPT_OCR,

    /**
     * Raw chat log pasted or forwarded from a messaging app.
     * Typically multi-line with mixed participant contributions.
     */
    CHAT_LOG,

    /**
     * Used in test harnesses and seeded demo data to bypass
     * signature validation and rate-limiting layers.
     */
    SYNTHETIC
}
