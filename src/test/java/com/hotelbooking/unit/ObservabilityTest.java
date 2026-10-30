package com.hotelbooking.unit;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.hotelbooking.shared.logging.FailureDiagnostics;
import com.hotelbooking.shared.logging.TransactionLog;
import com.hotelbooking.shared.web.TraceFilter;
import com.hotelbooking.support.TestTransactionManager;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.servlet.HandlerMapping;

import static org.assertj.core.api.Assertions.*;

@Tag("unit")
class ObservabilityTest {
    @Test
    void successCallbackRunsOnlyAfterCommitAndIsSkippedOnRollback() {
        var tx = new TransactionTemplate(new TestTransactionManager());
        var messages = new AtomicInteger();
        tx.executeWithoutResult(status -> {
            TransactionLog.afterCommit(messages::incrementAndGet);
            assertThat(messages.get()).isZero();
        });
        assertThat(messages.get()).isEqualTo(1);
        tx.executeWithoutResult(status -> {
            TransactionLog.afterCommit(messages::incrementAndGet);
            status.setRollbackOnly();
        });
        assertThat(messages.get()).isEqualTo(1);
    }

    @Test
    void requestLogsUseRoutePatternsAndExcludeQueryHeadersAndBody() throws Exception {
        var logger = (Logger) LoggerFactory.getLogger(TraceFilter.class);
        var appender = new ListAppender<ILoggingEvent>();
        appender.start();
        logger.addAppender(appender);
        String previous = MDC.get("traceId");
        try {
            MDC.put("traceId", "parent-trace");
            var request = new MockHttpServletRequest("POST", "/api/crm/guests/private-id/notes");
            request.setQueryString("email=private@example.com");
            request.addHeader("Authorization", "Basic private-password");
            request.addHeader("X-Trace-Id", "child-trace-123");
            request.setContent("private note and phone".getBytes());
            var response = new MockHttpServletResponse();
            new TraceFilter().doFilter(request, response, (req, res) -> {
                request.setAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, "/api/crm/guests/{id}/notes");
                response.setStatus(201);
            });
            var messages = appender.list.stream().map(ILoggingEvent::getFormattedMessage).toList();
            assertThat(messages).hasSize(2);
            assertThat(messages.getLast()).contains("status=201", "durationMs=", "route=/api/crm/guests/{id}/notes");
            assertThat(String.join("\n", messages)).doesNotContain("private-id", "private@example.com", "private-password",
                "private note");
            assertThat(response.getHeader("X-Trace-Id")).isEqualTo("child-trace-123");
            assertThat(MDC.get("traceId")).isEqualTo("parent-trace");
        } finally {
            logger.detachAppender(appender);
            if (previous == null) {
                MDC.remove("traceId");
            } else {
                MDC.put("traceId", previous);
            }
        }
    }

    @Test
    void diagnosticsKeepSourceLocationsButExcludeExceptionMessagesAndNestedMessages() {
        var failure = new IllegalStateException("private note", new java.sql.SQLException("private password",
            "23505", 42));
        assertThat(FailureDiagnostics.describe(failure)).contains("IllegalStateException", "SQLException",
            "ObservabilityTest", "sqlState=23505", "vendorCode=42")
            .doesNotContain("private note", "private password");
    }
}
