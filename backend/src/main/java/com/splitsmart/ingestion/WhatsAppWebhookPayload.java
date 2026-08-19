package com.splitsmart.ingestion;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Deserialization model for the WhatsApp Cloud API webhook notification payload.
 *
 * Meta delivers webhook events as HTTP POST requests containing one or more
 * Entry objects. Each entry may contain multiple Change objects describing
 * message receipts, status updates, or business-initiated events.
 *
 * Spec: https://developers.facebook.com/docs/whatsapp/cloud-api/webhooks/payload-examples
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class WhatsAppWebhookPayload {

    @JsonProperty("object")
    private String object;          // always "whatsapp_business_account"

    @JsonProperty("entry")
    private List<Entry> entry;

    public String getObject()      { return object; }
    public List<Entry> getEntry()  { return entry; }

    // ─── Nested: Entry ──────────────────────────────────────────────────────

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Entry {

        @JsonProperty("id")
        private String id;              // WABA ID

        @JsonProperty("changes")
        private List<Change> changes;

        public String getId()              { return id; }
        public List<Change> getChanges()   { return changes; }
    }

    // ─── Nested: Change ─────────────────────────────────────────────────────

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Change {

        @JsonProperty("value")
        private Value value;

        @JsonProperty("field")
        private String field;       // "messages"

        public Value getValue()    { return value; }
        public String getField()   { return field; }
    }

    // ─── Nested: Value ──────────────────────────────────────────────────────

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Value {

        @JsonProperty("messaging_product")
        private String messagingProduct;    // "whatsapp"

        @JsonProperty("metadata")
        private Metadata metadata;

        @JsonProperty("contacts")
        private List<Contact> contacts;

        @JsonProperty("messages")
        private List<Message> messages;

        @JsonProperty("statuses")
        private List<Status> statuses;

        public String getMessagingProduct()    { return messagingProduct; }
        public Metadata getMetadata()          { return metadata; }
        public List<Contact> getContacts()     { return contacts; }
        public List<Message> getMessages()     { return messages; }
        public List<Status> getStatuses()      { return statuses; }
    }

    // ─── Nested: Metadata ───────────────────────────────────────────────────

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Metadata {

        @JsonProperty("display_phone_number")
        private String displayPhoneNumber;

        @JsonProperty("phone_number_id")
        private String phoneNumberId;

        public String getDisplayPhoneNumber()  { return displayPhoneNumber; }
        public String getPhoneNumberId()       { return phoneNumberId; }
    }

    // ─── Nested: Contact ────────────────────────────────────────────────────

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Contact {

        @JsonProperty("profile")
        private Profile profile;

        @JsonProperty("wa_id")
        private String waId;

        public Profile getProfile()  { return profile; }
        public String getWaId()      { return waId; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Profile {

        @JsonProperty("name")
        private String name;

        public String getName() { return name; }
    }

    // ─── Nested: Message ────────────────────────────────────────────────────

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Message {

        @JsonProperty("from")
        private String from;            // E.164 phone number

        @JsonProperty("id")
        private String id;              // wamid

        @JsonProperty("timestamp")
        private String timestamp;       // Unix epoch string

        @JsonProperty("type")
        private String type;            // "text" | "image" | "document" | "audio" | "interactive"

        @JsonProperty("text")
        private TextBody text;

        @JsonProperty("image")
        private MediaBody image;

        @JsonProperty("document")
        private MediaBody document;

        @JsonProperty("audio")
        private MediaBody audio;

        public String getFrom()         { return from; }
        public String getId()           { return id; }
        public String getTimestamp()    { return timestamp; }
        public String getType()         { return type; }
        public TextBody getText()       { return text; }
        public MediaBody getImage()     { return image; }
        public MediaBody getDocument()  { return document; }
        public MediaBody getAudio()     { return audio; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TextBody {

        @JsonProperty("body")
        private String body;

        public String getBody() { return body; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MediaBody {

        @JsonProperty("id")
        private String id;

        @JsonProperty("mime_type")
        private String mimeType;

        @JsonProperty("sha256")
        private String sha256;

        @JsonProperty("caption")
        private String caption;

        @JsonProperty("filename")
        private String filename;

        public String getId()        { return id; }
        public String getMimeType()  { return mimeType; }
        public String getSha256()    { return sha256; }
        public String getCaption()   { return caption; }
        public String getFilename()  { return filename; }
    }

    // ─── Nested: Status ─────────────────────────────────────────────────────

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Status {

        @JsonProperty("id")
        private String id;

        @JsonProperty("status")
        private String status;      // "sent" | "delivered" | "read" | "failed"

        @JsonProperty("timestamp")
        private String timestamp;

        @JsonProperty("recipient_id")
        private String recipientId;

        public String getId()            { return id; }
        public String getStatus()        { return status; }
        public String getTimestamp()     { return timestamp; }
        public String getRecipientId()   { return recipientId; }
    }
}
