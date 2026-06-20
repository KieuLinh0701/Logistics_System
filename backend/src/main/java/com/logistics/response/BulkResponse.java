package com.logistics.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BulkResponse<T> {
    private boolean success;
    private String message;
    private int totalImported;
    private int totalFailed;
    private List<BulkResult<T>> results;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BulkResult<T> {
        private String name;
        private boolean success;
        private String message;
        private T result;
    }
}