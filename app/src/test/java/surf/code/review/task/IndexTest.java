package surf.code.review.task;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.io.IOException;
import java.util.List;
import java.util.NavigableMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.RepeatedTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;

class IndexTest {

    private static final String PATH_DIR = "src/test/resources/samples";

    private static final ExecutorService pool = Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors() + 2,
            new ThreadFactoryBuilder().setNameFormat("index-workers-%d").build()
    );


    @RepeatedTest(10)
    void indexAllTxtInPathTest() throws IOException, InterruptedException {

        Index index = new Index(pool);
        index.indexAllTxtInPathAsync(PATH_DIR)
                .forEach(CompletableFuture::join);

        NavigableMap<String, List<Pointer>> invertedIndex = index.getInvertedIndex();


        assertThat(invertedIndex.get("arman"),
                hasItems(
                        new Pointer("src/test/resources/samples/2.txt", 1),
                        new Pointer("src/test/resources/samples/1.txt", 1)
                ));
        assertThat(invertedIndex.get("da"),
                hasItems(
                        new Pointer("src/test/resources/samples/1.txt", 2),
                        new Pointer("src/test/resources/samples/2.txt", 1),
                        new Pointer("src/test/resources/samples/3.txt", 1)
                ));
    }
}