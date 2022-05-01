package ru.surf;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.IntStream;
import java.util.stream.Stream;


public class Index {

    // Все поля долны быть private
    private final TreeMap<String, List<Pointer>> invertedIndex;

    private final ExecutorService pool;

    public Index(ExecutorService pool) {

        this.pool = pool;
        invertedIndex = new TreeMap<>();

    }

    public void indexAllTxtInPath(String pathToDir) throws IOException {

        // Переменные необходимо именовать в соответствии с контекстом происходящего
        Path path = Path.of(pathToDir);

        // Capacity нужно вывести в отдельную переменную
        int capacityBlockingQueue = 2;

        BlockingQueue<Path> files = new ArrayBlockingQueue<>(capacityBlockingQueue);

        // Этот код может вызвать IllegalStateException, если в каталоге будет больше елементов, чем capacityBlockingQueue
/*        try (Stream<Path> stream = Files.list(path)) {
            stream.forEach(files::add);
        }*/

        try (Stream<Path> stream = Files.list(path)) {

            stream.forEach(e -> {

                try {

                    files.put(e);

                } catch (InterruptedException ex) {

                    System.out.println(ex.getMessage()); // Лучше использовать логгер
                    Thread.currentThread().interrupt();

                }
            });

        }

        pool.submit(new IndexTask(files));

        //После полнения необходимо закрыть поток
        pool.shutdown();

    }

    // Возвращаем интерфейс, чтобы пользователь сам выбрал реализацию
    public SortedMap<String, List<Pointer>> getInvertedIndex() {
        return invertedIndex;
    }

    // Название метода должно начинаться с сточной буквы (GetRelevantDocuments -> getRelevantDocuments)
    public List<Pointer> getRelevantDocuments(String term) {
        return invertedIndex.get(term);
    }

    public Optional<Pointer> getMostRelevantDocument(String term) {

        return invertedIndex.get(term).stream().max(Comparator.comparing(Pointer::getCount));

    }

    public static class Pointer {

        private Integer count;
        private final String filePath;

        public Pointer(Integer count, String filePath) {

            this.count = count;
            this.filePath = filePath;

        }

        // Для получения и установки значения поля необходимо добавить геттеры и сеттеры
        public Integer getCount() {
            return count;
        }

        public String getFilePath() {
            return filePath;
        }

        public void setCount(Integer count) {
            this.count = count;
        }

        // Стандартный toString должен содержать имя класса
        @Override
        public String toString() {

            return "Pointer{" +
                    "count=" + count +
                    ", filePath='" + filePath + '\'' +
                    '}';

        }
    }

    public class IndexTask implements Runnable {

        private final BlockingQueue<Path> queue;

        public IndexTask(BlockingQueue<Path> queue) {

            this.queue = queue;

        }

        @Override
        public void run() {
            try {
                Path take = queue.take();
                List<String> strings = Files.readAllLines(take);
                String takeToString = take.toString(); // Именьшение дублирования кода

                strings.stream()
                        .flatMap(str -> Stream.of(str.split(" ")))
                        .forEach(word -> invertedIndex.compute(word, (k, v) -> {

                            if (v == null) return List.of(new Pointer(1, takeToString));

                            else {

                                ArrayList<Pointer> pointers = new ArrayList<>();

                                if (v.stream().noneMatch(pointer -> pointer.getFilePath().equals(takeToString))) {

                                    pointers.add(new Pointer(1, takeToString));

                                }

                                v.forEach(pointer -> {

                                    if (pointer.getFilePath().equals(takeToString)) {

                                        pointer.setCount(pointer.getCount() + 1);

                                    }

                                });

                                pointers.addAll(v);

                                return pointers;
                            }

                        }));

            } /*catch (InterruptedException | IOException e) {
                // Необходмо обработать исключения
                //throw new RuntimeException();
            }*/
            catch (InterruptedException ex) {
                System.out.println(ex.getMessage());
                Thread.currentThread().interrupt();
            }
            catch (IOException exx) {
                System.out.println(exx.getMessage());
            }
        }
    }
}