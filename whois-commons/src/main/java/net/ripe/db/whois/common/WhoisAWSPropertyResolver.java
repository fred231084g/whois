package net.ripe.db.whois.common;

import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagement;
import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagementClient;
import com.amazonaws.services.simplesystemsmanagement.model.GetParametersByPathRequest;
import com.amazonaws.services.simplesystemsmanagement.model.GetParametersByPathResult;
import com.amazonaws.services.simplesystemsmanagement.model.Parameter;
import net.ripe.db.whois.common.profiles.WhoisProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

@Configuration
@Profile(WhoisProfile.AWS_DEPLOYED)
//TODO: remove this once all properties are stored in parameter store
@PropertySources({
        @PropertySource(value = "classpath:version.properties", ignoreResourceNotFound = true),
        @PropertySource(value = "classpath:whois.properties", ignoreResourceNotFound = true),
        @PropertySource(value = "file:${whois.config}", ignoreResourceNotFound = true),
})
public class WhoisAWSPropertyResolver {

    private static final Logger LOGGER = LoggerFactory.getLogger(WhoisAWSPropertyResolver.class);

    //TODO: get region to deploy from gitlab
    private static final String REGION = "eu-central-1";

    @Bean
    public static PropertySourcesPlaceholderConfigurer properties(){
        LOGGER.info("using aws parameter store for properties ");

        Properties properties = new Properties();
        properties.putAll( getParametersByEnvAndApp("/whois", true));

        PropertySourcesPlaceholderConfigurer propertySourceConfig = new PropertySourcesPlaceholderConfigurer();
        propertySourceConfig.setProperties(properties);

        return propertySourceConfig;
    }

    public static Map<String, Object> getParametersByEnvAndApp(final String path, final boolean encryption) {
        final AWSSimpleSystemsManagement awsSimpleSystemsManagement = AWSSimpleSystemsManagementClient.builder()
                .withRegion(REGION).build();

        final GetParametersByPathRequest getParametersByPathRequest = new GetParametersByPathRequest().withPath(path)
                .withWithDecryption(encryption)
                .withRecursive(true);

        String token = null;
        final Map<String, Object> params = new HashMap<>();

        do {
            getParametersByPathRequest.setNextToken(token);
            final GetParametersByPathResult parameterResult = awsSimpleSystemsManagement.getParametersByPath(getParametersByPathRequest);
            token = parameterResult.getNextToken();

            params.putAll(addParamsToMap(parameterResult.getParameters()));
            //TODO: remove this logger once finalized
            LOGGER.info("aws parameters are: " + params.toString());

        } while (token != null);

        return params;
    }

    private static Map<String,String> addParamsToMap(List<Parameter> parameters) {
        return parameters.stream()
                .collect(Collectors.toMap(
                        parameter -> parameter.getName().substring(parameter.getName().lastIndexOf("/") + 1),
                        Parameter::getValue
                        )
                );
    }
}