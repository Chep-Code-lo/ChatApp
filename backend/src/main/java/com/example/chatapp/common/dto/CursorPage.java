package com.example.chatapp.common.dto;

import  java.util.List;
public  record CursorPage<T> (List<T> items, String nextCursor, boolean hasMore) {
    public CursorPage {
        items = List.copyOf(items);
        hasMore = nextCursor != null;
    }
    public static <T> CursorPage<T> of(List<T> items, String nextCursor){
        return  new CursorPage<>(items, nextCursor, nextCursor != null);
    }
}