package com.chatbot.backend.service;

import com.chatbot.backend.domain.Message;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

@SpringBootTest
@Transactional
@Import(MessageHistoryServiceTrimTest.TrimConfig.class)
class MessageHistoryServiceTrimTest {

    @TestConfiguration
    static class TrimConfig {

        @Bean
        @Primary
        MessageHistoryOptions trimOptions() {
            return MessageHistoryOptions.builder()
                .maxMessages(4)
                .build();
        }
    }

    @Autowired
    MessageHistoryService messageHistory;

    @Autowired
    EntityManager em;

    @Test
    @DisplayName("최대 메시지 수를 초과하면 가장 오래된 user-assistant 쌍이 제거된다")
    void add_WhenExceedsMaxMessages_ThenOldestPairIsRemoved() {
        //given
        String sessionId = "session-trim-oldest";

        //when
        messageHistory.add(sessionId, "user", "u1");
        messageHistory.add(sessionId, "assistant", "a1");
        messageHistory.add(sessionId, "user", "u2");
        messageHistory.add(sessionId, "assistant", "a2");
        messageHistory.add(sessionId, "user", "u3");
        messageHistory.add(sessionId, "assistant", "a3");
        em.flush();
        em.clear();

        //then
        assertThat(messageHistory.get(sessionId))
            .extracting(Message::getRole, Message::getContent)
            .containsExactly(
                tuple("user", "u2"),
                tuple("assistant", "a2"),
                tuple("user", "u3"),
                tuple("assistant", "a3")
            );
    }

    @Test
    @DisplayName("트림으로 메시지를 제거할 때 user-assistant 쌍 단위가 유지되어 user 메시지가 단독으로 남지 않는다")
    void add_WhenTrimming_ThenPairsArePreservedAndUserNeverStandsAlone() {
        //given
        String sessionId = "session-trim-pairs";

        //when
        messageHistory.add(sessionId, "user", "u1");
        messageHistory.add(sessionId, "assistant", "a1");
        messageHistory.add(sessionId, "user", "u2");
        messageHistory.add(sessionId, "assistant", "a2");
        messageHistory.add(sessionId, "user", "u3");
        messageHistory.add(sessionId, "assistant", "a3");
        em.flush();
        em.clear();

        //then
        List<Message> messages = messageHistory.get(sessionId);
        assertThat(messages)
            .extracting(Message::getRole)
            .containsExactly("user", "assistant", "user", "assistant");
        assertThat(messages).hasSize(4);
    }
}
