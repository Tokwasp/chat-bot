package com.chatbot.backend.filter;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class RequestLoggingFilterTest {

    private final RequestLoggingFilter filter = new RequestLoggingFilter();

    private Logger logger;
    private ListAppender<ILoggingEvent> appender;

    @BeforeEach
    void attachAppender() {
        logger = (Logger) LoggerFactory.getLogger(RequestLoggingFilter.class);
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void detachAppender() {
        logger.detachAppender(appender);
    }

    @Test
    @DisplayName("다음 미들웨어로 제어를 넘기기 위해 chain.doFilter(next)를 호출한다")
    void doFilter_ThenCallsNext() throws Exception {
        //given
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/sessions");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        //when
        filter.doFilter(request, response, chain);

        //then
        verify(chain).doFilter(request, response);
    }

    @Test
    @DisplayName("체인 처리 후 응답이 완료되면 메서드/경로/상태코드를 로그로 출력한다")
    void doFilter_ThenLogsOnResponseFinish() throws Exception {
        //given
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/sessions");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);
        doAnswer(invocation -> {
            HttpServletResponse res = invocation.getArgument(1);
            res.setStatus(201);
            return null;
        }).when(chain).doFilter(any(), any());

        //when
        filter.doFilter(request, response, chain);

        //then
        assertThat(appender.list).hasSize(1);
        String message = appender.list.get(0).getFormattedMessage();
        assertThat(message)
            .contains("GET")
            .contains("/api/sessions")
            .contains("201");
    }
}
