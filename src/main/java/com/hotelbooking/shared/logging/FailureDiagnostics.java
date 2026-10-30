package com.hotelbooking.shared.logging;

import java.util.Arrays;
import java.sql.SQLException;
import java.util.Collections;
import java.util.IdentityHashMap;

/** Exception messages may contain SQL values, payloads or credentials; record types and code locations only. */
public final class FailureDiagnostics {
    private FailureDiagnostics() {
    }

    public static String describe(Throwable failure) {
        var result = new StringBuilder();
        var visited = Collections.newSetFromMap(new IdentityHashMap<Throwable, Boolean>());
        while (failure != null && visited.add(failure)) {
            if (!result.isEmpty()) {
                result.append(" causedBy=");
            }
            result.append(failure.getClass().getName()).append(" at=")
                .append(Arrays.toString(failure.getStackTrace()));
            if (failure instanceof SQLException sql) {
                result.append(" sqlState=").append(sql.getSQLState()).append(" vendorCode=").append(sql.getErrorCode());
            }
            failure = failure.getCause();
        }
        return result.toString();
    }
}
