package surf.code.review.task;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

// todo - add test
public class CompletableFutureFactory {
    private final ExecutorService executorService;

    public CompletableFutureFactory(ExecutorService executorService) {
        this.executorService = executorService;
    }

    public CompletableFuture<Void> runAsync(Runnable task) {
        return CompletableFuture.runAsync(task, executorService);
    }
}
