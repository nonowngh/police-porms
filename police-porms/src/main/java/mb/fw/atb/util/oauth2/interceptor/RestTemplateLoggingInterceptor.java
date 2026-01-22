package mb.fw.atb.util.oauth2.interceptor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.AbstractClientHttpResponse;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.StreamUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

@Slf4j
public class RestTemplateLoggingInterceptor implements ClientHttpRequestInterceptor {

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
        logRequest(request, body);
        ClientHttpResponse executeResponse = execution.execute(request, body);
        ClientHttpResponse response = logResponse(executeResponse);
        return response;
    }

    private void logRequest(HttpRequest request, byte[] body) throws IOException {

        if (log.isDebugEnabled()) {
            log.debug("===log request start===");
            log.debug("URI: {}", request.getURI());
            log.debug("Method: {}", request.getMethod());
            log.debug("Headers: {}", request.getHeaders());
            log.debug("Request body: {}", new String(body, "UTF-8"));
            log.debug("===log request end===");
        }
    }

    private ClientHttpResponse logResponse(ClientHttpResponse response) throws IOException {

        if (log.isDebugEnabled()) {
            log.debug("===log response start===");
            log.debug("Status code: {}", response.getStatusCode());
            log.debug("Status text: {}", response.getStatusText());
            log.debug("Headers: {}", response.getHeaders());
            String bodyStr = StreamUtils.copyToString(response.getBody(), Charset.defaultCharset());
            log.debug("Response body: {}", bodyStr);
            log.debug("===log response end===");

            ClientHttpResponse indirectResponse = new ClientHttpResponse() {
                @Override
                public HttpStatus getStatusCode() throws IOException {
                    return response.getStatusCode();
                }

                @Override
                public int getRawStatusCode() throws IOException {
                    return response.getRawStatusCode();
                }

                @Override
                public String getStatusText() throws IOException {
                    return response.getStatusText();
                }

                @Override
                public void close() {
                    response.close();
                }

                @Override
                public InputStream getBody() throws IOException {
                    return new ByteArrayInputStream(bodyStr.getBytes());
                }

                @Override
                public HttpHeaders getHeaders() {
                    return response.getHeaders();
                }
            };

            return indirectResponse;
        }

        return response;
    }
};
