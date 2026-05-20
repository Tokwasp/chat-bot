package com.chatbot.backend.config;

import com.chatbot.backend.service.MessageHistoryOptions;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MessageHistoryConfig {

    @Bean
    @ConditionalOnMissingBean
    public MessageHistoryOptions messageHistoryOptions() {
        return MessageHistoryOptions.builder().build();
    }
}
