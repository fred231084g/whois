package net.ripe.db.whois.api.rdap;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.Lists;
import net.ripe.db.whois.api.rdap.domain.RdapObject;
import org.glassfish.jersey.server.ParamException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
@Component
public class RdapExceptionMapper implements ExceptionMapper<Exception> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RdapExceptionMapper.class);

    private final RdapObjectMapper rdapObjectMapper;

    @Autowired
    public RdapExceptionMapper(
            final RdapObjectMapper rdapObjectMapper) {
        this.rdapObjectMapper = rdapObjectMapper;
    }

    @Override
    public Response toResponse(final Exception exception) {
        if (exception instanceof IllegalArgumentException || exception instanceof JsonProcessingException){
            return createErrorResponse(HttpServletResponse.SC_BAD_REQUEST, exception.getMessage());
        }
        if (exception instanceof ParamException){
            final String parameterName = ((ParamException) exception).getParameterName();
            return createErrorResponse(HttpServletResponse.SC_BAD_REQUEST, "400 Bad Request",
                    "unknown " + parameterName);
        }
        if (exception instanceof RdapException){
            final RdapException rdapException = (RdapException) exception;
            return createErrorResponse(rdapException.getErrorCode(), rdapException.getErrorTitle(),
                    rdapException.getErrorDescription() == null? "Unknown error cause" :
                            rdapException.getErrorDescription());
        }

        LOGGER.error("Unexpected", exception);
        return createErrorResponse(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, exception.getMessage());

    }

    private Response createErrorResponse(final int status, final String errorTitle, final String ... errorMessage) {
        return Response.status(status)
                .entity(createErrorEntity(status, errorTitle, errorMessage))
                .header(HttpHeaders.CONTENT_TYPE, "application/rdap+json")
                .build();
    }

    private RdapObject createErrorEntity(final int errorCode, final String errorTitle, final String ... errorTexts) {
        return rdapObjectMapper.mapError(errorCode, errorTitle, Lists.newArrayList(errorTexts));
    }
}
