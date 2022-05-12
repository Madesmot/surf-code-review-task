package surf.code.review.task;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class Index implements AutoCloseable {
    // or another logger
    private static final Logger LOGGER = Logger.getLogger("IndexLogger");
    private static final String WHITESPACE = "\\s+";

    private final NavigableMap<String, List<Pointer>> invertedIndex = new ConcurrentSkipListMap<>();
    private final ExecutorService pool;
    private final CompletableFutureFactory completableFutureFactory;

    public Index(ExecutorService pool) {
        this.pool = pool;
        this.completableFutureFactory = new CompletableFutureFactory(this.pool);
    }

    public List<CompletableFuture<Void>> indexAllTxtInPathAsync(String pathToDir) throws IOException {
        try (Stream<Path> stream = Files.list(Path.of(pathToDir))) {
            return stream.map(path -> completableFutureFactory.runAsync(new IndexTask(path)))
                .collect(Collectors.toList());
        }
    }

    public NavigableMap<String, List<Pointer>> getInvertedIndex() {
        return Collections.unmodifiableNavigableMap(invertedIndex);
    }

    public List<Pointer> getRelevantDocuments(String term) {
        return invertedIndex.get(term);
    }

    public Optional<Pointer> getMostRelevantDocument(String term) {
        return invertedIndex.get(term).stream().max(Comparator.comparing(Pointer::getCount));
    }

    @Override
    public void close() {
        invertedIndex.clear();
        pool.shutdown();
        try {
            if (!pool.awaitTermination(60, TimeUnit.SECONDS)) {
                pool.shutdownNow();
                if (!pool.awaitTermination(60, TimeUnit.SECONDS))
                    System.err.println("Pool did not terminate");
            }
        } catch (InterruptedException ie) {
            pool.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private class IndexTask implements Runnable {
        private final Path path;

        public IndexTask(Path path) {
            this.path = path;
        }

        @Override
        public void run() {
            try {
                String filePath = path.toString();
                Map<String, Long> wordContMap = Files.readAllLines(path).stream()
                        .flatMap(string -> Stream.of(string.split(WHITESPACE)))
                        .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

                wordContMap.forEach((word, count) -> {
                    invertedIndex.computeIfPresent(word, (w, pointers) -> {
                        List<Pointer> newPointers = pointers.stream()
                                //.filter(pointer -> !filePath.equals(pointer.getFilePath()))
                                .map(pointer -> {
                                    long oldCount = pointer.getCount();
                                    long newCount = count + oldCount;
                                    pointer.setCount(newCount);
                                    return new Pointer(filePath, newCount);
                                })
                                .collect(Collectors.toList());
                        newPointers.addAll(pointers);
                        return newPointers;
                    });

                    invertedIndex.computeIfAbsent(word, s -> List.of(new Pointer(filePath, count)));
                });
            } catch (IOException e) {
                Thread.currentThread().interrupt();
                // or another logger
                LOGGER.log(Level.WARNING, "Error while read file " + Thread.currentThread().getName() + " " + e.getMessage());
                // there are no reason to throw Runtime Exception
            }
        }
    }
}