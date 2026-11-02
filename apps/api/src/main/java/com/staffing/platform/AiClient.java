package com.staffing.platform;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import java.net.http.HttpClient;

@Component
public class AiClient {
    private final RestClient client;
    public AiClient(@Value("${app.ai-base-url}") String base) {
        var factory=new JdkClientHttpRequestFactory(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build());
        factory.setReadTimeout(Duration.ofSeconds(120));
        this.client=RestClient.builder().baseUrl(base).requestFactory(factory).build();
    }
    public Object parse(Models.ParseInput input) {return client.post().uri("/requirements/parse").body(input).retrieve().body(Object.class);}
    public Object health() {return client.get().uri("/health").retrieve().body(Object.class);}
    public Object evaluations() {return client.get().uri("/evaluations").retrieve().body(Object.class);}
}
