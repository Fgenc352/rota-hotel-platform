package com.hotelbooking.shared.web;

import com.hotelbooking.shared.exception.BusinessException;
import java.util.stream.Stream;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.dao.OptimisticLockingFailureException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class BusinessErrorMappingTest {
    static Stream<Arguments> errors() {
        return Stream.of(
            Arguments.of(BusinessException.invalid("invalid stay"), 400, "VALIDATION"),
            Arguments.of(BusinessException.missing(), 404, "NOT_FOUND"),
            Arguments.of(BusinessException.forbidden("NOTE_AUTHOR_REQUIRED", "not the author"), 403, "NOTE_AUTHOR_REQUIRED"),
            Arguments.of(BusinessException.conflict("SOLD_OUT", "sold out"), 409, "SOLD_OUT")
            );
    }

    @ParameterizedTest
    @MethodSource("errors")
    void mapsDomainErrorsAtTheHttpBoundary(BusinessException error, int status, String code) {
        var response = new ApiErrors().business(error);
        assertThat(response.getStatusCode().value()).isEqualTo(status);
        assertThat(response.getBody().getProperties()).containsEntry("code", code);
    }
    @Test
    void optimisticConflictIs409AndDoesNotExposePersistenceDetails() {
        var response = new ApiErrors().concurrentUpdate(new OptimisticLockingFailureException("private entity details"));
        assertThat(response.getStatusCode().value()).isEqualTo(409);
        assertThat(response.getBody().getProperties()).containsEntry("code", "CONCURRENT_MODIFICATION");
        assertThat(response.getBody().getDetail()).doesNotContain("private entity details");
    }

}
