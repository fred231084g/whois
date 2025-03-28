package net.ripe.db.whois.update.log;

import com.google.common.collect.Maps;
import net.ripe.db.whois.common.DateTimeProvider;
import net.ripe.db.whois.common.jdbc.driver.ResultInfo;
import net.ripe.db.whois.common.jdbc.driver.StatementInfo;
import net.ripe.db.whois.common.rpsl.RpslObject;
import net.ripe.db.whois.update.domain.Operation;
import net.ripe.db.whois.update.domain.Paragraph;
import net.ripe.db.whois.update.domain.Update;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AuditLoggerTest {
    @Spy ByteArrayOutputStream outputStream;
    @Mock DateTimeProvider dateTimeProvider;
    AuditLogger subject;

    private Update update;

    @BeforeEach
    public void setUp() throws Exception {
        when(dateTimeProvider.getCurrentDateTime()).thenReturn(LocalDateTime.of(2012, 12, 1, 0, 0));
        update = new Update(new Paragraph("paragraph"), Operation.DELETE, Arrays.asList("reason"), RpslObject.parse("mntner:DEV-ROOT-MNT"));

        subject = new AuditLogger(dateTimeProvider, outputStream);
    }

    @Test
    public void logUpdateStarted() throws Exception {
        subject.logUpdate(update);
        subject.close();

        final String log = outputStream.toString("UTF-8");
        assertThat(log, containsString("" +
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n" +
                "<dbupdate created=\"2012-12-01T00:00:00Z\">\n" +
                "    <messages/>\n" +
                "    <updates>\n" +
                "        <update attempt=\"1\" time=\"2012-12-01T00:00:00Z\">\n" +
                "            <key>[mntner] DEV-ROOT-MNT</key>\n" +
                "            <operation>DELETE</operation>\n" +
                "            <reason>reason</reason>\n" +
                "            <paragraph>\n" +
                "                <![CDATA[paragraph]]>\n" +
                "            </paragraph>\n" +
                "            <object>\n" +
                "                <![CDATA[mntner:         DEV-ROOT-MNT\n" +
                "]]>\n" +
                "            </object>\n" +
                "        </update>\n" +
                "    </updates>\n" +
                "</dbupdate>\n"));
    }

    @Test
    public void logUpdateStarted_twice() throws Exception {
        subject.logUpdate(update);
        subject.logUpdate(update);
        subject.close();

        final String log = outputStream.toString("UTF-8");
        assertThat(log, is("" +
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n" +
                "<dbupdate created=\"2012-12-01T00:00:00Z\">\n" +
                "    <messages/>\n" +
                "    <updates>\n" +
                "        <update attempt=\"2\" time=\"2012-12-01T00:00:00Z\">\n" +
                "            <key>[mntner] DEV-ROOT-MNT</key>\n" +
                "            <operation>DELETE</operation>\n" +
                "            <reason>reason</reason>\n" +
                "            <paragraph>\n" +
                "                <![CDATA[paragraph]]>\n" +
                "            </paragraph>\n" +
                "            <object>\n" +
                "                <![CDATA[mntner:         DEV-ROOT-MNT\n" +
                "]]>\n" +
                "            </object>\n" +
                "        </update>\n" +
                "    </updates>\n" +
                "</dbupdate>\n"));
    }

    @Test
    public void logException() throws Exception {
        subject.logUpdate(update);
        subject.logException(update, new NullPointerException());
        subject.close();

        final String log = outputStream.toString("UTF-8");
        assertThat(log, containsString("" +
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n" +
                "<dbupdate created=\"2012-12-01T00:00:00Z\">\n" +
                "    <messages/>\n" +
                "    <updates>\n" +
                "        <update attempt=\"1\" time=\"2012-12-01T00:00:00Z\">\n" +
                "            <key>[mntner] DEV-ROOT-MNT</key>\n" +
                "            <operation>DELETE</operation>\n" +
                "            <reason>reason</reason>\n" +
                "            <paragraph>\n" +
                "                <![CDATA[paragraph]]>\n" +
                "            </paragraph>\n" +
                "            <object>\n" +
                "                <![CDATA[mntner:         DEV-ROOT-MNT\n" +
                "]]>\n" +
                "            </object>\n" +
                "            <exception>\n" +
                "                <class>java.lang.NullPointerException</class>\n" +
                "                <message>\n" +
                "                    <![CDATA[null]]>\n" +
                "                </message>\n" +
                "                <stacktrace>\n" +
                "                    <![CDATA[java.lang.NullPointerException\n"));
    }

    @Test
    public void logDuration() throws Exception {
        subject.logUpdate(update);
        subject.logDuration(update, "1 ns");
        subject.close();

        final String log = outputStream.toString("UTF-8");
        assertThat(log, containsString("" +
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n" +
                "<dbupdate created=\"2012-12-01T00:00:00Z\">\n" +
                "    <messages/>\n" +
                "    <updates>\n" +
                "        <update attempt=\"1\" time=\"2012-12-01T00:00:00Z\">\n" +
                "            <key>[mntner] DEV-ROOT-MNT</key>\n" +
                "            <operation>DELETE</operation>\n" +
                "            <reason>reason</reason>\n" +
                "            <paragraph>\n" +
                "                <![CDATA[paragraph]]>\n" +
                "            </paragraph>\n" +
                "            <object>\n" +
                "                <![CDATA[mntner:         DEV-ROOT-MNT\n" +
                "]]>\n" +
                "            </object>\n" +
                "            <duration>1 ns</duration>\n" +
                "        </update>\n" +
                "    </updates>\n" +
                "</dbupdate>"));
    }

    @Test
    public void logQuery() throws Exception {
        final Map<Integer, Object> params = Maps.newTreeMap();
        params.put(1, "p1");
        params.put(2, 22);

        final List<List<String>> rows = Arrays.asList(
                Arrays.asList("c1-1", "c1-2"),
                Arrays.asList("c2-1", "c2-2"));

        final StatementInfo statementInfo = new StatementInfo("sql", params);
        final ResultInfo resultInfo = new ResultInfo(rows);

        subject.logUpdate(update);
        subject.logQuery(update, statementInfo, resultInfo);
        subject.close();

        final String log = outputStream.toString("UTF-8");
        assertThat(log, containsString("" +
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n" +
                "<dbupdate created=\"2012-12-01T00:00:00Z\">\n" +
                "    <messages/>\n" +
                "    <updates>\n" +
                "        <update attempt=\"1\" time=\"2012-12-01T00:00:00Z\">\n" +
                "            <key>[mntner] DEV-ROOT-MNT</key>\n" +
                "            <operation>DELETE</operation>\n" +
                "            <reason>reason</reason>\n" +
                "            <paragraph>\n" +
                "                <![CDATA[paragraph]]>\n" +
                "            </paragraph>\n" +
                "            <object>\n" +
                "                <![CDATA[mntner:         DEV-ROOT-MNT\n" +
                "]]>\n" +
                "            </object>\n" +
                "            <query>\n" +
                "                <sql>\n" +
                "                    <![CDATA[sql]]>\n" +
                "                </sql>\n" +
                "                <params>\n" +
                "                    <param idx=\"1\">p1</param>\n" +
                "                    <param idx=\"2\">22</param>\n" +
                "                </params>\n" +
                "                <results>\n" +
                "                    <row idx=\"1\">\n" +
                "                        <column idx=\"0\">\n" +
                "                            <![CDATA[c1-1]]>\n" +
                "                        </column>\n" +
                "                        <column idx=\"1\">\n" +
                "                            <![CDATA[c1-2]]>\n" +
                "                        </column>\n" +
                "                    </row>\n" +
                "                    <row idx=\"2\">\n" +
                "                        <column idx=\"0\">\n" +
                "                            <![CDATA[c2-1]]>\n" +
                "                        </column>\n" +
                "                        <column idx=\"1\">\n" +
                "                            <![CDATA[c2-2]]>\n" +
                "                        </column>\n" +
                "                    </row>\n" +
                "                </results>\n" +
                "            </query>\n" +
                "        </update>\n" +
                "    </updates>\n" +
                "</dbupdate>"));
    }

    @Test
    public void empty() throws Exception {
        subject.close();

        assertThat(outputStream.toString("UTF-8"), is("" +
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n" +
                "<dbupdate created=\"2012-12-01T00:00:00Z\">\n" +
                "    <messages/>\n" +
                "    <updates/>\n" +
                "</dbupdate>\n"
        ));

        verify(outputStream, times(1)).close();
    }
}
