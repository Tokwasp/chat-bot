package com.chatbot.backend.service;

import com.chatbot.backend.domain.Session;
import com.chatbot.backend.dto.CreateSessionRequest;
import com.chatbot.backend.dto.UpdateSessionRequest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class SessionManagerTest {

    @Autowired
    SessionManager sessionManager;

    @Autowired
    EntityManager em;

    @Test
    @DisplayName("세션을 생성하면 고유 ID, 기본 제목('New Chat'), 생성/수정 시각이 자동으로 설정된다")
    void create_WhenNoIdAndTitle_ThenAutoFilled() {
        //given
        CreateSessionRequest request = createSessionRequest(null, null, null);

        //when
        Session session = sessionManager.create(request);

        //then
        assertThat(session.getId()).isNotBlank();
        assertThat(session.getTitle()).isEqualTo("New Chat");
        assertThat(session.getCreatedAt()).isNotNull();
        assertThat(session.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("커스텀 제목과 메타데이터를 전달하면 그 값이 반영된 세션이 생성된다")
    void create_WhenCustomTitleAndMetadata_ThenStored() {
        //given
        Map<String, Object> metadata = Map.of("model", "claude-sonnet-4-6");
        CreateSessionRequest request = createSessionRequest(null, "내 챗방", metadata);

        //when
        Session session = sessionManager.create(request);

        //then
        assertThat(session.getTitle()).isEqualTo("내 챗방");
        assertThat(session.getMetadata()).isEqualTo(metadata);
    }

    @Test
    @DisplayName("ID로 세션을 조회하면 해당 세션을 반환한다")
    void get_WhenIdExists_ThenReturnsSession() {
        //given
        Session created = sessionManager.create(createSessionRequest(null, "조회 대상", null));

        //when
        Optional<Session> found = sessionManager.get(created.getId());

        //then
        assertThat(found).isPresent();
        assertThat(found.get())
            .extracting(Session::getId, Session::getTitle)
            .containsExactly(created.getId(), "조회 대상");
    }

    @Test
    @DisplayName("존재하지 않는 ID로 조회하면 빈 Optional을 반환한다")
    void get_WhenIdDoesNotExist_ThenReturnsEmpty() {
        //when
        Optional<Session> found = sessionManager.get("non-existent-id");

        //then
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("전체 세션 목록은 수정 시각 기준 최신순으로 정렬된다")
    void getAll_ThenSortedByUpdatedAtDesc() throws InterruptedException {
        //given
        Session first = sessionManager.create(createSessionRequest(null, "첫번째", null));
        Thread.sleep(10);
        Session second = sessionManager.create(createSessionRequest(null, "두번째", null));
        Thread.sleep(10);
        Session third = sessionManager.create(createSessionRequest(null, "세번째", null));
        em.flush();
        em.clear();

        //when
        List<Session> all = sessionManager.getAll();

        //then
        assertThat(all)
            .extracting(Session::getId)
            .containsExactly(third.getId(), second.getId(), first.getId());
    }

    @Test
    @DisplayName("세션의 제목과 메타데이터를 수정하면 변경되고 수정 시각이 갱신된다")
    void update_WhenTitleAndMetadata_ThenUpdatedAtChanges() throws InterruptedException {
        //given
        Session created = sessionManager.create(
            createSessionRequest(null, "원래 제목", Map.of("k", "v")));
        LocalDateTime originalUpdatedAt = created.getUpdatedAt();
        Map<String, Object> newMetadata = Map.of("model", "haiku");
        Thread.sleep(10);

        //when
        sessionManager.update(created.getId(), createUpdateRequest("변경된 제목", newMetadata));
        em.flush();
        em.clear();

        //then
        Session updated = sessionManager.get(created.getId()).orElseThrow();
        assertThat(updated.getTitle()).isEqualTo("변경된 제목");
        assertThat(updated.getMetadata()).isEqualTo(newMetadata);
        assertThat(updated.getUpdatedAt()).isAfter(originalUpdatedAt);
    }

    @Test
    @DisplayName("세션을 삭제하면 더 이상 조회되지 않는다")
    void delete_WhenSessionExists_ThenNotFoundAfterDelete() {
        //given
        Session created = sessionManager.create(createSessionRequest(null, "삭제 대상", null));
        em.flush();
        em.clear();

        //when
        boolean deleted = sessionManager.delete(created.getId());
        em.flush();
        em.clear();

        //then
        assertThat(deleted).isTrue();
        assertThat(sessionManager.get(created.getId())).isEmpty();
    }

    private static CreateSessionRequest createSessionRequest(String id, String title, Map<String, Object> metadata) {
        return new CreateSessionRequest(id, title, metadata);
    }

    private static UpdateSessionRequest createUpdateRequest(String title, Map<String, Object> metadata) {
        return new UpdateSessionRequest(title, metadata);
    }
}
