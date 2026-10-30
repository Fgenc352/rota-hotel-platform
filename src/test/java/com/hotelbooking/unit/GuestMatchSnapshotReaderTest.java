package com.hotelbooking.unit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotelbooking.guest.infrastructure.GuestMatchSnapshotReader;
import com.hotelbooking.shared.json.Json;
import com.hotelbooking.shared.persistence.JpaQueries;
import jakarta.persistence.Tuple;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Tag("unit")
class GuestMatchSnapshotReaderTest {
    @Test
    void captureWrapsSourceAndTargetDocumentsWithoutSwappingThem() throws Exception {
        var queries = mock(JpaQueries.class);
        var mapper = new ObjectMapper();
        var reader = new GuestMatchSnapshotReader(queries, new Json(mapper));
        UUID hotel = UUID.randomUUID();
        UUID source = UUID.randomUUID();
        UUID target = UUID.randomUUID();
        String sourceJson = "{\"profile\":{\"id\":\"source\"},\"notes\":[{\"body\":\"Original note\"}]}";
        String targetJson = "{\"profile\":{\"id\":\"target\"},\"notes\":[]}";
        var sourceRow = row(sourceJson);
        var targetRow = row(targetJson);
        when(queries.tuples(anyString(), eq(Map.of("hotel", hotel, "guest", source))))
            .thenReturn(List.of(sourceRow));
        when(queries.tuples(anyString(), eq(Map.of("hotel", hotel, "guest", target))))
            .thenReturn(List.of(targetRow));

        var document = mapper.readTree(reader.capture(hotel, source, target));
        assertThat(document.path("schemaVersion").asInt()).isEqualTo(1);
        assertThat(document.path("source")).isEqualTo(mapper.readTree(sourceJson));
        assertThat(document.path("target")).isEqualTo(mapper.readTree(targetJson));
    }

    @Test
    void missingProfileFailsInsteadOfWritingAnIncompleteSnapshot() {
        var queries = mock(JpaQueries.class);
        when(queries.tuples(anyString(), anyMap())).thenReturn(List.of());
        var reader = new GuestMatchSnapshotReader(queries, new Json(new ObjectMapper()));
        assertThatThrownBy(() -> reader.capture(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID()))
            .isInstanceOf(IllegalStateException.class);
    }

    private Tuple row(String json) {
        var row = mock(Tuple.class);
        when(row.get("snapshot")).thenReturn(json);
        return row;
    }
}
