package com.chatbot.backend.service;

import com.chatbot.backend.domain.Message;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

@SpringBootTest
@Transactional
class MessageHistoryServiceTest {

    @Autowired
    MessageHistoryService messageHistory;

    @Autowired
    EntityManager em;

    @Test
    @DisplayName("사용자 메시지와 어시스턴트 메시지를 추가하면 추가한 순서대로 조회된다")
    void add_UserAndAssistantMessages_ThenReturnedInOrder() {
        //given
        String sessionId = "session-1";

        //when
        messageHistory.add(sessionId, "user", "안녕");
        messageHistory.add(sessionId, "assistant", "안녕하세요");
        em.flush();
        em.clear();

        //then
        List<Message> messages = messageHistory.get(sessionId);
        assertThat(messages)
            .extracting(Message::getRole, Message::getContent)
            .containsExactly(
                tuple("user", "안녕"),
                tuple("assistant", "안녕하세요")
            );
    }

    @Test
    @DisplayName("세션마다 독립된 히스토리를 유지해 서로 다른 세션의 메시지가 섞이지 않는다")
    void get_WhenMultipleSessions_ThenHistoriesAreIsolated() {
        //given
        String sessionA = "session-A";
        String sessionB = "session-B";
        messageHistory.add(sessionA, "user", "A의 첫번째");
        messageHistory.add(sessionB, "user", "B의 첫번째");
        messageHistory.add(sessionA, "assistant", "A에 대한 답변");
        em.flush();
        em.clear();

        //when
        List<Message> sessionAMessages = messageHistory.get(sessionA);
        List<Message> sessionBMessages = messageHistory.get(sessionB);

        //then
        assertThat(sessionAMessages)
            .extracting(Message::getContent)
            .containsExactly("A의 첫번째", "A에 대한 답변");
        assertThat(sessionBMessages)
            .extracting(Message::getContent)
            .containsExactly("B의 첫번째");
    }

    @Test
    @DisplayName("존재하지 않는 세션을 조회하면 빈 리스트를 반환한다")
    void get_WhenSessionDoesNotExist_ThenReturnsEmptyList() {
        //when
        List<Message> messages = messageHistory.get("non-existent-session");

        //then
        assertThat(messages).isEmpty();
    }

    @Test
    @DisplayName("같은 세션에 여러 메시지를 추가하면 추가된 순서 그대로 반환된다")
    void get_WhenMultipleMessages_ThenReturnsInInsertionOrder() {
        //given
        String sessionId = "session-order";
        messageHistory.add(sessionId, "user", "첫번째");
        messageHistory.add(sessionId, "assistant", "두번째");
        messageHistory.add(sessionId, "user", "세번째");
        messageHistory.add(sessionId, "assistant", "네번째");
        em.flush();
        em.clear();

        //when
        List<Message> messages = messageHistory.get(sessionId);

        //then
        assertThat(messages)
            .extracting(Message::getContent)
            .containsExactly("첫번째", "두번째", "세번째", "네번째");
    }

    @Test
    @DisplayName("getWithLimit으로 최근 N개의 메시지만 조회할 수 있다")
    void getWithLimit_WhenLimitGiven_ThenReturnsLastN() {
        //given
        String sessionId = "session-limit";
        messageHistory.add(sessionId, "user", "1");
        messageHistory.add(sessionId, "assistant", "2");
        messageHistory.add(sessionId, "user", "3");
        messageHistory.add(sessionId, "assistant", "4");
        messageHistory.add(sessionId, "user", "5");
        messageHistory.add(sessionId, "assistant", "6");
        em.flush();
        em.clear();

        //when
        List<Message> last3 = messageHistory.getWithLimit(sessionId, 3);

        //then
        assertThat(last3)
            .extracting(Message::getContent)
            .containsExactly("4", "5", "6");
    }

    @Test
    @DisplayName("특정 세션의 히스토리를 초기화하면 해당 세션 메시지만 비워지고 다른 세션은 그대로 유지된다")
    void clear_WhenSessionHasMessages_ThenOtherSessionsAreNotAffected() {
        //given
        String targetSession = "session-target";
        String otherSession = "session-other";
        messageHistory.add(targetSession, "user", "삭제될 메시지");
        messageHistory.add(targetSession, "assistant", "삭제될 답변");
        messageHistory.add(otherSession, "user", "유지될 메시지");
        em.flush();
        em.clear();

        //when
        messageHistory.clear(targetSession);
        em.flush();
        em.clear();

        //then
        assertThat(messageHistory.get(targetSession)).isEmpty();
        assertThat(messageHistory.get(otherSession))
            .extracting(Message::getContent)
            .containsExactly("유지될 메시지");
    }
}
