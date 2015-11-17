package net.ripe.db.whois.changedphase3.util;

import net.ripe.db.whois.api.MailUpdatesTestSupport;
import net.ripe.db.whois.api.rest.mapper.WhoisObjectMapper;
import net.ripe.db.whois.nrtm.NrtmServer;
import net.ripe.db.whois.update.mail.MailSenderStub;

public class Context {
    private int restPort;
    private int syncUpdatePort;
    private WhoisObjectMapper whoisObjectMapper;
    private boolean debug = false;
    private MailUpdatesTestSupport mailUpdatesTestSupport;
    private MailSenderStub mailSenderStub;
    private NrtmServer nrtmServer;

    public Context(int restPort, int syncUpdatePort, WhoisObjectMapper whoisObjectMapper,
                   MailUpdatesTestSupport mailUpdatesTestSupport, MailSenderStub mailSenderStub,
                   NrtmServer nrtmServer) {
        this.restPort = restPort;
        this.syncUpdatePort = syncUpdatePort;
        this.whoisObjectMapper = whoisObjectMapper;
        this.mailUpdatesTestSupport = mailUpdatesTestSupport;
        this.mailSenderStub = mailSenderStub;
        this.nrtmServer = nrtmServer;
    }

    public int getRestPort() {
        return restPort;
    }

    public int getSyncUpdatePort() {
        return syncUpdatePort;
    }

    public WhoisObjectMapper getWhoisObjectMapper() {
        return whoisObjectMapper;
    }

    public MailUpdatesTestSupport getMailUpdatesTestSupport() {
        return mailUpdatesTestSupport;
    }

    public MailSenderStub getMailSenderStub() {
        return mailSenderStub;
    }

    public NrtmServer getNrtmServer() {
        return nrtmServer;
    }

    public boolean isDebug() {
        return debug;
    }

}
