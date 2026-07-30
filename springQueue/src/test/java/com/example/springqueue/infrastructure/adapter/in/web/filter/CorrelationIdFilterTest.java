package com.example.springqueue.infrastructure.adapter.in.web.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("Given a CorrelationIdFilter")
class CorrelationIdFilterTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    private final CorrelationIdFilter filter = new CorrelationIdFilter();

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Nested
    @DisplayName("When traceId is present in MDC")
    class WhenTraceIdPresent {

        private static final String TRACE_ID = "abc-123";

        @BeforeEach
        void setUp() {
            MDC.put("traceId", TRACE_ID);
        }

        @Test
        @DisplayName("given traceId abc-123, then should use it as correlationId in MDC")
        void shouldUseTraceIdAsCorrelationId() throws Exception {
            doAnswer(invocation -> {
                assertEquals(TRACE_ID, MDC.get("correlationId"));
                return null;
            }).when(filterChain).doFilter(request, response);

            filter.doFilterInternal(request, response, filterChain);
        }

        @Test
        @DisplayName("given traceId abc-123, then should set X-Correlation-Id header")
        void shouldSetHeaderWithTraceId() throws Exception {
            filter.doFilterInternal(request, response, filterChain);

            verify(response).setHeader("X-Correlation-Id", TRACE_ID);
        }

        @Test
        @DisplayName("given traceId abc-123, then should invoke filter chain")
        void shouldInvokeDoFilter() throws Exception {
            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
        }
    }

    @Nested
    @DisplayName("When traceId is absent from MDC")
    class WhenTraceIdAbsent {

        @Test
        @DisplayName("given no traceId, then should generate UUID as correlationId in MDC")
        void shouldGenerateUUIDAsFallback() throws Exception {
            doAnswer(invocation -> {
                String correlationId = MDC.get("correlationId");
                assertNotNull(correlationId);
                assertDoesNotThrow(() -> UUID.fromString(correlationId));
                return null;
            }).when(filterChain).doFilter(request, response);

            filter.doFilterInternal(request, response, filterChain);
        }

        @Test
        @DisplayName("given no traceId, then should set X-Correlation-Id header")
        void shouldSetHeaderWithUUID() throws Exception {
            filter.doFilterInternal(request, response, filterChain);

            verify(response).setHeader(eq("X-Correlation-Id"), anyString());
        }

        @Test
        @DisplayName("given no traceId, then should invoke filter chain")
        void shouldInvokeDoFilter() throws Exception {
            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
        }
    }

    @Nested
    @DisplayName("After filter execution")
    class AfterExecution {

        @Test
        @DisplayName("given any scenario, then should remove correlationId from MDC")
        void shouldRemoveCorrelationIdFromMdc() throws Exception {
            MDC.put("correlationId", "temp");

            filter.doFilterInternal(request, response, filterChain);

            assertNull(MDC.get("correlationId"));
        }
    }
}
