package ru.surf;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Stream;

public class Index {
    /*
    Внутренние переменные классы желательно делать закрытыми к изменениям извне
     */
    private final TreeMap<String, List<Pointer>> invertedIndex;

    private final ExecutorService pool;

    public Index(ExecutorService pool) {
        this.pool = pool;
        invertedIndex = new TreeMap<>();
    }

    public void indexAllTxtInPath(String pathToDir) throws IOException {
        Path path = Path.of(pathToDir);

        BlockingQueue<Path> files = new ArrayBlockingQueue<>(2);

        try (Stream<Path> stream = Files.list(path)) {
            stream.forEach(files::add);
        }

        pool.submit(new IndexTask(files));
        pool.submit(new IndexTask(files));
        pool.submit(new IndexTask(files));
    }

    /*
    Здесь должна быть коллекция SortedMap вместо TreeMap, так как в качестве типа возвращаемого значение нужно ставить
    интерфейс, а не конкретную реализацию коллекции
     */
    public SortedMap<String, List<Pointer>> getInvertedIndex() {
        return invertedIndex;
    }

    /*
    Имя метода должно начинаться с маленькой буквы, согласно camelCase
     */
    public List<Pointer> getRelevantDocuments(String term) {
        return invertedIndex.get(term);
    }

    public Optional<Pointer> getMostRelevantDocument(String term) {
        /*
        Для лучшего вида кода
         */
        return invertedIndex.get(term).stream()
                .max(Comparator.comparing(o -> o.count));
    }

    static class Pointer {
        private Integer count;
        private final String filePath;

        public Pointer(Integer count, String filePath) {
            this.count = count;
            this.filePath = filePath;
        }

        @Override
        public String toString() {
            return "{" +
                    "count=" + count +
                    ", filePath='" + filePath + '\'' +
                    "}";
        }
    }

    class IndexTask implements Runnable {

        private final BlockingQueue<Path> queue;

        public IndexTask(BlockingQueue<Path> queue) {
            this.queue = queue;
        }

        @Override
        public void run() {
            try {
                Path take = queue.take();
                List<String> strings = Files.readAllLines(take);

                strings.stream()
                        .flatMap(str -> Stream.of(str.split(" ")))
                        .forEach(word -> invertedIndex.compute(word,
                                (k, v) -> {
                                    if (v == null) {
                                        return List.of(new Pointer(1, take.toString()));
                                    } else {
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
            /*
            Исключение Interrupted не должно обрабатываться подобным образом, иначе информация о том, почему поток был
            прерван будет потеряна и не удастся узнать причин прерывания потока
             */
            } catch (InterruptedException | IOException e) {
                /*
                Чтобы такого не было, нужно дополнительно прервать поток, чтобы исключение не осталось упущенным
                 */
                Thread.currentThread().interrupt();
                /*
                Не стоит использовать общий класс Runtime Exception при обработке исключения, нужно создать свой для
                конкретной ошибки и пробрасывать туда хоть какую-то информацию, а не оставлять пустым поле сообщение
                 */
                throw new RuntimeException();
            }
        }
    }
}