package com.fesi.deadlinemate.global.monitoring;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

public final class QueryCountContext {

    private static final ThreadLocal<QueryCountContext> HOLDER = new ThreadLocal<>();

    // SQL 텍스트 → [실행횟수, 누적시간ms]
    private final Map<String, long[]> sqlStats = new LinkedHashMap<>();

    private QueryCountContext() {}

    public static void initialize() {
        HOLDER.set(new QueryCountContext());
    }

    public static QueryCountContext current() {
        return HOLDER.get();
    }

    public static void clear() {
        HOLDER.remove();
    }

    public void recordSql(String sql, long ms) {
        sqlStats.merge(sql, new long[]{1, ms},
                (prev, v) -> new long[]{prev[0] + 1, prev[1] + ms});
    }

    public boolean hasN1() {
        return sqlStats.values().stream().anyMatch(v -> v[0] >= 2);
    }

    public long getTotalCount() {
        return sqlStats.values().stream().mapToLong(v -> v[0]).sum();
    }

    public long getTotalTimeMs() {
        return sqlStats.values().stream().mapToLong(v -> v[1]).sum();
    }
}
