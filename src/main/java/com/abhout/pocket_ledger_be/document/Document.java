package com.abhout.pocket_ledger_be.document;

import com.abhout.pocket_ledger_be.user.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "documents")
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;
    private String filename;
    private String mimeType;
    private long size;
    private String objectKey;

    @Column(columnDefinition = "text")
    private DocumentStatus status;

    @Column(columnDefinition = "text")
    private DocumentSource source;
    private Instant createdAt = Instant.now();

    Document(
            User user,
            String filename,
            String mimeType,
            long size,
            String objectKey,
            DocumentStatus status,
            DocumentSource source) {
        this.user = user;
        this.filename = filename;
        this.mimeType = mimeType;
        this.size = size;
        this.objectKey = objectKey;
        this.status = status;
        this.source = source;
    }
}
