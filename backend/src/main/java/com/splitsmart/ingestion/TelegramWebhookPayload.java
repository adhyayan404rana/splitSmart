package com.splitsmart.ingestion;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Deserialization model for the Telegram Bot API webhook update payload.
 *
 * Telegram delivers a POST request to the registered webhook URL whenever
 * a new Update arrives (message, callback_query, inline_query, etc.).
 *
 * Spec: https://core.telegram.org/bots/api#update
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class TelegramWebhookPayload {

    @JsonProperty("update_id")
    private long updateId;

    @JsonProperty("message")
    private Message message;

    @JsonProperty("callback_query")
    private CallbackQuery callbackQuery;

    // ─── Getters ────────────────────────────────────────────────────────────

    public long getUpdateId()            { return updateId; }
    public Message getMessage()          { return message; }
    public CallbackQuery getCallbackQuery() { return callbackQuery; }

    // ─── Nested: Message ────────────────────────────────────────────────────

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Message {

        @JsonProperty("message_id")
        private long messageId;

        @JsonProperty("from")
        private User from;

        @JsonProperty("chat")
        private Chat chat;

        @JsonProperty("date")
        private long date;          // Unix timestamp

        @JsonProperty("text")
        private String text;

        @JsonProperty("photo")
        private List<PhotoSize> photo;

        @JsonProperty("document")
        private Document document;

        public long getMessageId()         { return messageId; }
        public User getFrom()              { return from; }
        public Chat getChat()              { return chat; }
        public long getDate()              { return date; }
        public String getText()            { return text; }
        public List<PhotoSize> getPhoto()  { return photo; }
        public Document getDocument()      { return document; }
    }

    // ─── Nested: User ───────────────────────────────────────────────────────

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class User {

        @JsonProperty("id")
        private long id;

        @JsonProperty("first_name")
        private String firstName;

        @JsonProperty("last_name")
        private String lastName;

        @JsonProperty("username")
        private String username;

        @JsonProperty("language_code")
        private String languageCode;

        public long getId()              { return id; }
        public String getFirstName()     { return firstName; }
        public String getLastName()      { return lastName; }
        public String getUsername()      { return username; }
        public String getLanguageCode()  { return languageCode; }

        public String displayName() {
            String name = firstName != null ? firstName : "";
            if (lastName != null && !lastName.isBlank()) name += " " + lastName;
            return name.isBlank() ? (username != null ? "@" + username : String.valueOf(id)) : name.trim();
        }
    }

    // ─── Nested: Chat ───────────────────────────────────────────────────────

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Chat {

        @JsonProperty("id")
        private long id;

        @JsonProperty("type")
        private String type;            // "private" | "group" | "supergroup" | "channel"

        @JsonProperty("title")
        private String title;

        @JsonProperty("username")
        private String username;

        public long getId()          { return id; }
        public String getType()      { return type; }
        public String getTitle()     { return title; }
        public String getUsername()  { return username; }
    }

    // ─── Nested: PhotoSize ──────────────────────────────────────────────────

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PhotoSize {

        @JsonProperty("file_id")
        private String fileId;

        @JsonProperty("file_unique_id")
        private String fileUniqueId;

        @JsonProperty("width")
        private int width;

        @JsonProperty("height")
        private int height;

        @JsonProperty("file_size")
        private long fileSize;

        public String getFileId()        { return fileId; }
        public String getFileUniqueId()  { return fileUniqueId; }
        public int getWidth()            { return width; }
        public int getHeight()           { return height; }
        public long getFileSize()        { return fileSize; }
    }

    // ─── Nested: Document ───────────────────────────────────────────────────

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Document {

        @JsonProperty("file_id")
        private String fileId;

        @JsonProperty("file_name")
        private String fileName;

        @JsonProperty("mime_type")
        private String mimeType;

        @JsonProperty("file_size")
        private long fileSize;

        public String getFileId()    { return fileId; }
        public String getFileName()  { return fileName; }
        public String getMimeType()  { return mimeType; }
        public long getFileSize()    { return fileSize; }
    }

    // ─── Nested: CallbackQuery ──────────────────────────────────────────────

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CallbackQuery {

        @JsonProperty("id")
        private String id;

        @JsonProperty("from")
        private User from;

        @JsonProperty("data")
        private String data;

        @JsonProperty("message")
        private Message message;

        public String getId()       { return id; }
        public User getFrom()       { return from; }
        public String getData()     { return data; }
        public Message getMessage() { return message; }
    }
}
