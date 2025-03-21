package net.ripe.db.whois.api.search;

import net.ripe.db.whois.api.fulltextsearch.IndexTemplate;
import net.ripe.db.whois.api.fulltextsearch.IndexTemplate.SearchCallback;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.facet.taxonomy.TaxonomyWriter;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopScoreDocCollector;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.fail;

public class IndexTemplateTest {

    @TempDir
    public File folder;

    IndexTemplate subject;
    WhitespaceAnalyzer analyzer;

    @BeforeEach
    public void setUp() throws Exception {
        analyzer = new WhitespaceAnalyzer();

        IndexWriterConfig config = new IndexWriterConfig(analyzer);
        config.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
        subject = new IndexTemplate(folder.getAbsolutePath(), config);
    }

    @AfterEach
    public void tearDown() {
        subject.close();
    }

    @Test
    public void index_and_search() throws IOException, ParseException {
        subject.write((indexWriter, taxonomyWriter) -> {
            addDoc(indexWriter, "Lucene in Action", "193398817");
            addDoc(indexWriter, "Lucene for Dummies", "55320055Z");
            addDoc(indexWriter, "Managing Gigabytes", "55063554A");
            addDoc(indexWriter, "The Art of Computer Science", "9900333X");

            assertThat(indexWriter.getPendingNumDocs(), is(4L));
        });

        final Query query = new QueryParser("title", analyzer).parse("Lucene");
        subject.search((SearchCallback<Void>) (indexReader, taxonomyReader, indexSearcher) -> {
            final TopScoreDocCollector collector = TopScoreDocCollector.create(10, Integer.MAX_VALUE);
            indexSearcher.search(query, collector);
            final ScoreDoc[] hits = collector.topDocs().scoreDocs;

            assertThat(hits.length, is(2));
            assertThat(indexReader.document(hits[0].doc).get("isbn"), is("193398817"));
            assertThat(indexReader.document(hits[1].doc).get("isbn"), is("55320055Z"));
            return null;
        });
    }

    @Test
    public void index_concurrent() throws Exception {
        final int nrThreads = 4;
        final CountDownLatch countDownLatch = new CountDownLatch(nrThreads);
        final ExecutorService executorService = Executors.newFixedThreadPool(nrThreads);

        assertThat(numDocs(), is(0));

        for (int i = 0; i < nrThreads; i++) {
            executorService.submit(new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    try {
                        subject.write(new IndexTemplate.WriteCallback() {
                            @Override
                            public void write(final IndexWriter indexWriter, final TaxonomyWriter taxonomyWriter) throws IOException {
                                addDoc(indexWriter, toString(), toString());
                            }
                        });
                    } finally {
                        countDownLatch.countDown();
                    }

                    return null;
                }
            });
        }

        countDownLatch.await(5, TimeUnit.SECONDS);
        assertThat(numDocs(), is(4));
    }

    @Test
    public void search_concurrent() throws Exception {
        final long nrDocs = 1000;
        final int nrThreads = 100;

        subject.write((indexWriter, taxonomyWriter) -> {
            for (int i = 0; i < nrDocs; i++) {
                addDoc(indexWriter, "title: " + i, "isbn: " + i);
            }

            assertThat(indexWriter.getPendingNumDocs(), is(nrDocs));
        });

        final CountDownLatch countDownLatch = new CountDownLatch(nrThreads);
        final ExecutorService executorService = Executors.newFixedThreadPool(nrThreads);
        final Query query = new QueryParser("title", analyzer).parse("title");
        for (int i = 0; i < nrThreads; i++) {
            executorService.submit((Callable<Void>) () -> {
                try {
                    subject.search((SearchCallback<Void>) (indexReader, taxonomyReader, indexSearcher) -> {
                        final TopScoreDocCollector collector = TopScoreDocCollector.create(10, Integer.MAX_VALUE);
                        indexSearcher.search(query, collector);
                        assertThat(collector.topDocs().scoreDocs.length, is(nrDocs));
                        return null;
                    });
                } finally {
                    countDownLatch.countDown();
                }

                return null;
            });
        }

        countDownLatch.await(10, TimeUnit.SECONDS);
    }

    @Test
    public void out_of_memory() throws IOException {
        try {
            subject.write((indexWriter, taxonomyWriter) -> {
                addDoc(indexWriter, "title", "isbn");
                throw new OutOfMemoryError();
            });

            fail("Expected exception");
        } catch (OutOfMemoryError ignored) {
        }

        // no rollback on out of memory, document was written properly
        assertThat(numDocs(), is(1));

        subject.write((indexWriter, taxonomyWriter) -> addDoc(indexWriter, "title", "isbn"));

        assertThat(numDocs(), is(2));
    }

    @Test
    public void runtime_exception() throws IOException {
        try {
            subject.write((indexWriter, taxonomyWriter) -> {
                addDoc(indexWriter, "title", "isbn");
                throw new IllegalStateException();
            });

            fail("Expected exception");
        } catch (IllegalStateException ignored) {
        }

        assertThat(numDocs(), is(0));

        subject.write((indexWriter, taxonomyWriter) -> addDoc(indexWriter, "title", "isbn"));

        assertThat(numDocs(), is(1));
    }

    @Test
    public void io_exception() throws IOException {
        try {
            subject.write((indexWriter, taxonomyWriter) -> {
                addDoc(indexWriter, "title", "isbn");
                throw new IOException();
            });

            fail("Expected exception");
        } catch (IOException ignored) {
        }

        assertThat(numDocs(), is(0));

        subject.write((indexWriter, taxonomyWriter) -> addDoc(indexWriter, "title", "isbn"));

        assertThat(numDocs(), is(1));
    }

    @Test
    public void refresh() throws IOException {
        assertThat(numDocs(), is(0));

        for (int i = 1; i <= 10; i++) {
            subject.write((indexWriter, taxonomyWriter) -> addDoc(indexWriter, "title", "isbn"));

            assertThat(numDocs(), is(i));
        }
    }

    int numDocs() throws IOException {
        return subject.read((indexReader, taxonomyReader) -> indexReader.numDocs());
    }

    void addDoc(final IndexWriter indexWriter, final String title, final String isbn) throws IOException {
        Document doc = new Document();
        doc.add(new TextField("title", title, Field.Store.YES));
        doc.add(new StringField("isbn", isbn, Field.Store.YES));
        indexWriter.addDocument(doc);
    }
}
