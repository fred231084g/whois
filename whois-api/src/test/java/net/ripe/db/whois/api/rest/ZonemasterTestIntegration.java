package net.ripe.db.whois.api.rest;

import net.ripe.db.whois.api.AbstractIntegrationTest;
import net.ripe.db.whois.api.ZonemasterDummy;
import net.ripe.db.whois.common.rpsl.RpslObject;
import net.ripe.db.whois.update.dns.DnsCheckRequest;
import net.ripe.db.whois.update.dns.zonemaster.ZonemasterRestClient;
import net.ripe.db.whois.update.dns.zonemaster.domain.StartDomainTestRequest;
import net.ripe.db.whois.update.dns.zonemaster.domain.StartDomainTestResponse;
import net.ripe.db.whois.update.dns.zonemaster.domain.VersionInfoRequest;
import net.ripe.db.whois.update.dns.zonemaster.domain.VersionInfoResponse;
import net.ripe.db.whois.update.domain.Credentials;
import net.ripe.db.whois.update.domain.Operation;
import net.ripe.db.whois.update.domain.Paragraph;
import net.ripe.db.whois.update.domain.Update;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@Tag("IntegrationTest")
public class ZonemasterTestIntegration extends AbstractIntegrationTest {

    @Autowired
    private ZonemasterDummy zonemasterDummy;
    @Autowired
    private ZonemasterRestClient zonemasterRestClient;

    @Test
    public void version_info() {
        zonemasterDummy.whenThen("1", "{\"jsonrpc\":\"2.0\",\"id\":1,\"result\":{\"zonemaster_backend\":\"1.1.0\"," +
                "\"zonemaster_engine\":\"v1.0.16\"}}\n");

        final VersionInfoResponse response = zonemasterRestClient.sendRequest(new VersionInfoRequest()).readEntity(VersionInfoResponse.class);

        assertThat(response.getId(), is("1"));
        assertThat(response.getResult().get("zonemaster_backend"), is("1.1.0"));
    }

    @Test
    public void start_domain_test_when_dsrdata_digest_contains_spaces() {
        zonemasterDummy.whenThen("1.0.10.in-addr.arpa", "{\"jsonrpc\":\"2.0\",\"id\":4,\"result\":\"b3a92c89c92414ed\"}\n");
        final RpslObject domainObject = RpslObject.parse(
            "domain:    1.0.10.in-addr.arpa\n" +
            "nserver:   ns1.ripe.net\n" +
            "nserver:   ns2.ripe.net\n" +
            "ds-rdata:  45123 10 2 76B64430CB85EA74E92184B9AF1F75482577237A4A5C23784AF9D2C1 7639088E\n" +
            "source:    TEST");
        final DnsCheckRequest request = new DnsCheckRequest(createUpdate(domainObject), domainObject.getKey().toString(), "ns1.ripe.net/10.0.1.1 ns2.ripe.net/10.0.1.2");

        final StartDomainTestResponse response = zonemasterRestClient.sendRequest(new StartDomainTestRequest(request)).readEntity(StartDomainTestResponse.class);

        assertThat(response.getResult(), is("b3a92c89c92414ed"));
    }
    @Test
    public void start_domain_test_with_trailing_dot() {
        zonemasterDummy.whenThen("1.0.10.in-addr.arpa", "{\"jsonrpc\":\"2.0\",\"id\":4,\"result\":\"b3a92c89c92414ed\"}\n");
        final RpslObject domainObject = RpslObject.parse(
                "domain:    1.0.10.in-addr.arpa.\n" +
                        "nserver:   ns1.ripe.net\n" +
                        "nserver:   ns2.ripe.net\n" +
                        "ds-rdata:  45123 10 2 76B64430CB85EA74E92184B9AF1F75482577237A4A5C23784AF9D2C1 7639088E\n" +
                        "source:    TEST");
        final DnsCheckRequest request = new DnsCheckRequest(createUpdate(domainObject), domainObject.getKey().toString(), "ns1.ripe.net/10.0.1.1 ns2.ripe.net/10.0.1.2");

        final StartDomainTestResponse response = zonemasterRestClient.sendRequest(new StartDomainTestRequest(request)).readEntity(StartDomainTestResponse.class);

        assertThat(response.getResult(), is("b3a92c89c92414ed"));
    }


    @Test
    public void start_domain_test_with_bad_domain() {
        final RpslObject domainObject = RpslObject.parse(
                "domain:    bad.domain.in-addr.arpa\n" +
                        "nserver:   ns1.ripe.net\n" +
                        "nserver:   ns2.ripe.net\n" +
                        "ds-rdata:  45123 10 2 76B64430CB85EA74E92184B9AF1F75482577237A4A5C23784AF9D2C1 7639088E\n" +
                        "source:    TEST");
        final DnsCheckRequest request = new DnsCheckRequest(createUpdate(domainObject), domainObject.getKey().toString(), "ns1.ripe.net/10.0.1.1 ns2.ripe.net/10.0.1.2");

        try {
            zonemasterRestClient.sendRequest(new StartDomainTestRequest(request)).readEntity(StartDomainTestResponse.class);
        } catch (IllegalStateException e){
            Assertions.assertTrue(e.getMessage().contains("domain:    bad.domain.in-addr.arpa"));
        }

    }
    // helper methods

    private static Update createUpdate(final RpslObject rpslObject) {
        final Paragraph paragraph = new Paragraph(rpslObject.toString(), new Credentials());
        return new Update(paragraph, Operation.UNSPECIFIED, null, rpslObject);
    }
}
