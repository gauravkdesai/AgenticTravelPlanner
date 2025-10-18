package com.agentictravel.services;

import java.util.concurrent.CompletableFuture;

public interface Agent<T> {
    CompletableFuture<T> search(Object request);
}
