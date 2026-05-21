package com.chatbot.backend.domain;

import com.chatbot.backend.dto.UpdateSessionRequest;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "sessions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Session {

    @Id
    private String id;

    @Column(nullable = false)
    private String title;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Convert(converter = MetadataConverter.class)
    @Column(columnDefinition = "TEXT")
    private Map<String, Object> metadata;

    public Session(String id, String title, Map<String, Object> metadata) {
        this.id = id;
        this.title = title;
        this.metadata = metadata;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    public void touch() {
        this.updatedAt = LocalDateTime.now();
    }

    public void applyUpdate(UpdateSessionRequest request) {
        if (StringUtils.hasText(request.getTitle())) {
            this.title = request.getTitle();
        }
        if (request.getMetadata() != null) {
            this.metadata = request.getMetadata();
        }
        this.updatedAt = LocalDateTime.now();
    }
}
