package mb.fw.atb.configuration;

import lombok.extern.slf4j.Slf4j;
import mb.fw.atb.config.IFConfig;
import mb.fw.atb.config.sub.Oauth2;
import mb.fw.atb.util.oauth2.OAuth2RestTemplate;
import mb.fw.atb.util.oauth2.interceptor.RestTemplateLoggingInterceptor;
import mb.fw.net.common.constant.AdaptorPropPrefix;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.*;
import org.springframework.security.oauth2.client.DefaultOAuth2ClientContext;
import org.springframework.security.oauth2.client.resource.OAuth2ProtectedResourceDetails;
import org.springframework.security.oauth2.client.token.AccessTokenRequest;
import org.springframework.security.oauth2.client.token.DefaultAccessTokenRequest;
import org.springframework.security.oauth2.client.token.grant.client.ClientCredentialsAccessTokenProvider;
import org.springframework.security.oauth2.client.token.grant.client.ClientCredentialsResourceDetails;
import org.springframework.security.oauth2.common.AuthenticationScheme;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.RestOperations;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;

/**
 * OAuth2RestTemplateConfig
 * OAuth2RestTemplate 설정
 * 현재는 클라이언트 자격증명만 구현함
 *
 * @version 1.0.0
 */
@Configuration
@Slf4j
@ConditionalOnProperty(name = "atb.if-config.oauth2", matchIfMissing = false)
public class OAuth2RestTemplateConfig {

    @Autowired
    IFConfig ifConfig;

    @Bean
    protected OAuth2ProtectedResourceDetails oauth2Resource() {

        Oauth2 oauth2 = ifConfig.getOauth2();
        String tokenUrl = oauth2.getAccessTokenUri();
        String clientId = oauth2.getClientId();
        String clientSecret = oauth2.getClientSecret();

        ClientCredentialsResourceDetails clientCredentialsResourceDetails = new ClientCredentialsResourceDetails();
        clientCredentialsResourceDetails.setAccessTokenUri(tokenUrl);
        clientCredentialsResourceDetails.setClientId(clientId);
        clientCredentialsResourceDetails.setClientSecret(clientSecret);
        clientCredentialsResourceDetails.setGrantType("client_credentials");
        clientCredentialsResourceDetails.setClientAuthenticationScheme(AuthenticationScheme.form);
        clientCredentialsResourceDetails.setAuthenticationScheme(AuthenticationScheme.form);
        log.info("clientCredentialsResourceDetails : " + clientCredentialsResourceDetails.toString());
        log.info("clientCredentialsResourceDetails : " + clientCredentialsResourceDetails.getAuthenticationScheme());
        return clientCredentialsResourceDetails;
    }

    @Bean
    public OAuth2RestTemplate oauth2RestTemplate(OAuth2ProtectedResourceDetails oauth2Resource) throws InstantiationException, IllegalAccessException, NoSuchMethodException, NoSuchFieldException {
        AccessTokenRequest atr = new DefaultAccessTokenRequest();
        OAuth2RestTemplate oauth2RestTemplate = new OAuth2RestTemplate(oauth2Resource, new DefaultOAuth2ClientContext(atr));
        oauth2RestTemplate.setRequestFactory(new HttpComponentsClientHttpRequestFactory());
        oauth2RestTemplate.getInterceptors().add(new RestTemplateLoggingInterceptor());

        return oauth2RestTemplate;
    }
}