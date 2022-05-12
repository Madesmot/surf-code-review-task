package ru.surf;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class Index {
    // or another logger
    private static final Logger LOGGER = Logger.getLogger("IndexLogger");
    private static final String WHITESPACE = "\\s+";

    private final NavigableMap<String, List<Pointer>> invertedIndex = new ConcurrentSkipListMap<>();
    private final ExecutorService pool;

    public Index(ExecutorService pool) {
        this.pool = pool;
    }

    public void indexAllTxtInPath(String pathToDir) throws IOException {
        try (Stream<Path> stream = Files.list(Path.of(pathToDir))) {
            stream.forEach(path -> pool.submit(new IndexTask(path)));
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

    private class IndexTask implements Runnable {
        private final Path path;

        public IndexTask(Path path) {
            this.path = path;
        }

        @Override
        public void run() {
            try {
                String pathString = path.toString();
                Files.readAllLines(path).stream()
                        .flatMap(string -> Stream.of(string.split(WHITESPACE)))
                        .forEach(word -> invertedIndex.compute(
                                word, (w, pointers) -> {
                                    if (pointers == null) {
                                        return List.of(new Pointer(pathString, 1));
                                    }

                                    List<Pointer> newPointers = pointers.stream()
                                            .filter(pointer -> pointer.getFilePath().equals(pathString))
                                            .map(pointer -> {
                                                long pointerCount = pointer.getCount();
                                                return new Pointer(pointer.getFilePath(), pointerCount + 1);
                                            })
                                            .collect(Collectors.toList());

                                    if (pointers.stream()
                                            .noneMatch(pointer -> pointer.getFilePath().equals(pathString))) {
                                        newPointers.add(new Pointer(pathString, 1));
                                    }

                                    newPointers.addAll(pointers);
                                    return newPointers;
                                }
                        ));

            } catch (IOException e) {
                Thread.currentThread().interrupt();
                // or another logger
                LOGGER.log(Level.WARNING, "Error while read file " + Thread.currentThread().getName() + " " + e.getMessage());
                // there are no reason to throw Runtime Exception
            }
        }
    }
}