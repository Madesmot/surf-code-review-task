package ru.surf;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Stream;

/**
 * Данный алгоритм подсчитывает количество вхождений слов в файлах
 * pathToDir каталог с текстовыми файлами
 */
// Лучше каждый класс выносить в новый файл
public class Index {
    // Все параметры должны быть привытаными, если нужен доступ с наружи то для этого есть гетерры и сеттеры
    TreeMap<String, List<Pointer>> invertedIndex;

    ExecutorService pool;

    public Index(ExecutorService pool) {
        this.pool = pool;
        // Нужно использовать this.invertedIndex для вызова полей класса
        invertedIndex = new TreeMap<>();
    }

    public void indexAllTxtInPath(String pathToDir) throws IOException {
        // Имена переменных должны быть осмыслены, не достаточно назвать переменную, оно должно быть говорящее
        Path of = Path.of(pathToDir);

        BlockingQueue<Path> files = new ArrayBlockingQueue<>(2);

        // В данным случае вылетит exception если будет больше чем 2 файла в пути, в конструкцию try добавить ограничение очереди stream.count()
        try (Stream<Path> stream = Files.list(of)) {
            // А если файл будет не текстовой, а другой?
            stream.forEach(files::add);
        }

        // Лучше ввести константу, и избавиться от копи-паста кода
        pool.submit(new IndexTask(files));
        pool.submit(new IndexTask(files));
        pool.submit(new IndexTask(files));
    }

    // Коллекция может быть переопределена из вне
    public TreeMap<String, List<Pointer>> getInvertedIndex() {
        return invertedIndex;
    }

    // Коллекция может быть переопределена из вне, не соблюдается установленное соглашение имен для методов
    public List<Pointer> GetRelevantDocuments(String term) {
        return invertedIndex.get(term);
    }

    public Optional<Pointer> getMostRelevantDocument(String term) {
        // get(term) может выбросить NullPointerException
        // Заставить каждый метод начинаться с новой строки
        return invertedIndex.get(term).stream().max(Comparator.comparing(o -> o.count));
    }

    static class Pointer {
        private Integer count;
        private String filePath;

        public Pointer(Integer count, String filePath) {
            this.count = count;
            this.filePath = filePath;
        }

        @Override
        public String toString() {
            // Добавить имя класса в вывод
            return "{" + "count=" + count + ", filePath='" + filePath + '\'' + '}';
        }
    }

    // Должна быть обеспечена минимальная видимость, если не используется из вне
    class IndexTask implements Runnable {

        private final BlockingQueue<Path> queue;

        public IndexTask(BlockingQueue<Path> queue) {
            this.queue = queue;
        }

        @Override
        public void run() {
            try {
                // Переменная должна быть говорящая
                Path take = queue.take();
                List<String> strings = Files.readAllLines(take);

                // Используйте регулярные выражения чтобы проигнорировать символы
                strings.stream().flatMap(str -> Stream.of(str.split(" "))).forEach(word -> invertedIndex.compute(word, (k, v) -> {
                    // Сделать переход на новоу строку, и обернуть блок if скобками для лучшей читаемости кода
                    if (v == null) return List.of(new Pointer(1, take.toString()));
                    else {
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

            } catch (InterruptedException | IOException e) {
                // Лучше обрабатывать исключения по отдельности
                // Требуется сообщить исполнителю что поток был прерван для этого нужно использовать Метод Thread.interrupt()
                throw new RuntimeException();
            }
        }
    }
}