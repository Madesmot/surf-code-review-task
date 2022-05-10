package ru.surf;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
//avoid * import
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Stream;

/**
 *  The class here is used to hold an index of text corpus using the format like: <word; <frequency; filePath>>
 * showing how many times the word occurs in the file.
 */

//make variables and methods' parameters final everywhere
public class Index {
    //can be private
    TreeMap<String, List<Pointer>> invertedIndex;

    //can be private
    //find the right place to shut id down
    ExecutorService pool;

    public Index(ExecutorService pool) {
        this.pool = pool;
        //use "this"
        invertedIndex = new TreeMap<>();
    }

    /**
     * Creates the index itself by reading the content of files from the parameter dir
     * @param pathToDir
     * @throws IOException
     */
    public void indexAllTxtInPath(String pathToDir) throws IOException {
        Path of = Path.of(pathToDir);

        //it can't be true, since there can be more than 2 files in the directory, which will lead to the exception
        //when trying to add the third one. Either check the "files" variable size and throw something in case if we
        //expect only two elements or find the files count in advance before creating the BlockingQueue
        BlockingQueue<Path> files = new ArrayBlockingQueue<>(2);
        try (Stream<Path> stream = Files.list(of)) {
            stream.forEach(files::add);
        }

        //Does it really make sense to run it 3 times?
        pool.submit(new IndexTask(files));
        pool.submit(new IndexTask(files));
        pool.submit(new IndexTask(files));
    }

    //use generic SortedMap, but not specific implementation
    //should we really return the index? What if it will be changed somewhere?
    //should throw kind of an exception in case if indexAllTxtInPath() wasn't yet executed
    public TreeMap<String, List<Pointer>> getInvertedIndex() {
        return invertedIndex;
    }

    //Naming - should start with "g"
    //should throw kind of an exception in case if indexAllTxtInPath() wasn't yet executed

    /**
     * returns all the documents containing the word with the word's frequence in the document
     * @param term
     * @return
     */
    public List<Pointer> GetRelevantDocuments(String term) {
        //invertedIndex.get() might be nullable
        return invertedIndex.get(term);
    }

    //should throw kind of an exception in case if indexAllTxtInPath() wasn't yet executed

    /**
     * returns the document having the highest frequency of the word with the frequency
     * @param term
     * @return
     */
    public Optional<Pointer> getMostRelevantDocument(String term) {
        //invertedIndex.get() might be nullable
        return invertedIndex.get(term).stream().max(Comparator.comparing(o -> o.count));
    }

    /**
     * Util class used to hold the word frequency and file path
     */
    static class Pointer {
        //normal get/set methods would be better
        private Integer count;
        private String filePath;

        public Pointer(Integer count, String filePath) {
            this.count = count;
            this.filePath = filePath;
        }

        @Override
        public String toString() {
            return "{" + "count=" + count + ", filePath='" + filePath + '\'' + '}';
        }
    }

    /**
     * Does the index calculation with the file splitting by words
     */
    class IndexTask implements Runnable {

        private final BlockingQueue<Path> queue;

        public IndexTask(BlockingQueue<Path> queue) {
            this.queue = queue;
        }

        @Override
        public void run() {
            try {
                //naming - call the path a bit more obvious
                Path take = queue.take();
                List<String> strings = Files.readAllLines(take);

                //readability - split stream chain by dots
                strings.stream().flatMap(str -> Stream.of(str.split(" "))).forEach(word -> invertedIndex.compute(word, (k, v) -> {
                    //think of calculation be simplified with just a HashMap<String, Integer> of <word; count> using
                    //putIfAbsent() / computeIfPresent()

                    //use curly brackets
                    //take.toString() used 4! times. Can be a variable
                    if (v == null) return List.of(new Pointer(1, take.toString()));
                    else {
                        //use generic List
                        ArrayList<Pointer> pointers = new ArrayList<>();

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

            }
            //Should be handled separately. Use CurrentThread.interrupt() when catching
            catch (InterruptedException | IOException e) {
                //The exception's cause was eliminated. Use custom exception instead of Runtime one
                throw new RuntimeException();
            }
        }
    }
}