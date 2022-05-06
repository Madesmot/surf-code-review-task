package ru.surf;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Stream;


public class Index {
    // FIXME: 06.05.2022 make variables private if they're not used in outer scope
    TreeMap<String, List<Pointer>> invertedIndex;

    ExecutorService pool;

    public Index(ExecutorService pool) {
        this.pool = pool;
        // FIXME: 06.05.2022 stay consistent: use this.invertedIndex for class fields initialization
        // FIXME: 06.05.2022 Should be wrapped with Collections#synchronizedSortedMap due to concurrent access (invertedIndex.compute)
        invertedIndex = new TreeMap<>();
    }

    /**
     *  This algorithm builds the following index in parallel:
     *  word -> list of files where it has been found + number of occurrences in each file
     * @param pathToDir directory with text files to be scanned.
     */
    public void indexAllTxtInPath(String pathToDir) throws IOException {
        // FIXME: 06.05.2022 use proper variables name: e.g. Path path
        Path of = Path.of(pathToDir);

        BlockingQueue<Path> files = new ArrayBlockingQueue<>(2);

        // FIXME: 06.05.2022 java.util.concurrent.BlockingQueue.add will throw an exception in case when there will be
        // more than 2 files in the path. Better to set capacity of the queue equal to stream.count() inside try-block
        try (Stream<Path> stream = Files.list(of)) {
            // FIXME: 06.05.2022 what if there will be non-text file?
            stream.forEach(files::add);
        }

        // FIXME: 06.05.2022 do DEGREE_OF_PARALLELISM iterations (introduce const) to avoid code copy-paste
        pool.submit(new IndexTask(files));
        pool.submit(new IndexTask(files));
        pool.submit(new IndexTask(files));
    }

    // FIXME: 06.05.2022 keep in mind that collection is mutable and could be overridden from outside
    public TreeMap<String, List<Pointer>> getInvertedIndex() {
        return invertedIndex;
    }

    // FIXME: 06.05.2022 check java naming conventions for methods: https://www.oracle.com/java/technologies/javase/codeconventions-namingconventions.html
    // "... with the first letter lowercase ..."
    // FIXME: 06.05.2022 keep in mind that collection is mutable and could be overridden from outside
    public List<Pointer> GetRelevantDocuments(String term) {
        return invertedIndex.get(term);
    }

    public Optional<Pointer> getMostRelevantDocument(String term) {
        // FIXME: 06.05.2022 split chained calls making each method starting from new line
        // FIXME: 06.05.2022 get(term) could provide NPE
        return invertedIndex.get(term).stream().max(Comparator.comparing(o -> o.count));
    }

    // FIXME: 06.05.2022 class Pointer should be replaced with Map<String, Integer>: filePath -> count
    static class Pointer {
        private Integer count;
        private String filePath;

        public Pointer(Integer count, String filePath) {
            this.count = count;
            this.filePath = filePath;
        }

        @Override
        public String toString() {
            // FIXME: 06.05.2022 Add class name to the output
            return "{" + "count=" + count + ", filePath='" + filePath + '\'' + '}';
        }
    }

    // FIXME: 06.05.2022 make inner classes private if they're not used in outer scope
    class IndexTask implements Runnable {

        private final BlockingQueue<Path> queue;

        public IndexTask(BlockingQueue<Path> queue) {
            this.queue = queue;
        }

        @Override
        // FIXME: 06.05.2022 the processing logic should be moved to a separate method to have an ability to cover code with unit tests
        public void run() {
            try {
                // FIXME: 06.05.2022 rename variable: Path element = ...
                Path take = queue.take();
                List<String> strings = Files.readAllLines(take);

                // FIXME: 06.05.2022 splitting by space doesn't take into account multiple spaces in a row and punctuation thus calculation will be dirty
                // I assume to use regexp \\W+ to ignore all non-alphabetic characters
                strings.stream().flatMap(str -> Stream.of(str.split(" "))).forEach(word -> invertedIndex.compute(word, (k, v) -> {
                    // FIXME: 06.05.2022 wrap if blocks with brackets even if it's one-liner
                    // FIXME: 06.05.2022 move return to the new line to improve readability
                    if (v == null) return List.of(new Pointer(1, take.toString()));
                    else {
                        // FIXME: 06.05.2022 use interfaces instead of implementations for variables definition
                        ArrayList<Pointer> pointers = new ArrayList<>();


                        // FIXME: 06.05.2022 having Map<String, Integer> (filePath -> count) instead of List<Pointer> will simplify the calculations:
                        // file2count.compute(filePath, (file, count) -> {
                        //     if (count == null) {
                        //         return 1;
                        //     } else {
                        //         return count + 1;
                        //     }
                        // });
                        if (v.stream().noneMatch(pointer -> pointer.filePath.equals(take.toString()))) {
                            pointers.add(new Pointer(1, take.toString()));
                        }

                        v.forEach(pointer -> {
                            if (pointer.filePath.equals(take.toString())) {
                                pointer.count = pointer.count + 1;
                            }
                        });

                        pointers.addAll(v);

                        return pointers;
                    }

                }));

            } catch (InterruptedException | IOException e) {
                // FIXME: 06.05.2022 split catch sections, use Thread.currentThread().interrupt() to let executor know the thread was interrupted
                // FIXME: 06.05.2022 pass cause exception into RuntimeException constructor
                throw new RuntimeException();
            }
        }
    }
}
