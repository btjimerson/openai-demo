package dev.snbv2.ai;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Configuration class for the application.
 * 
 * @author Brian Jimerson
 */
@Configuration
public class OpenAiDemoConfiguration {

    @Value("${openai.api-key}")
    String openAiApiKey;

    @Value("${openai.base-url}")
    String openAiBaseUrl;

    /**
     * Creates a REST client builder with custom interceptors to be used by chat
     * clients.
     * 
     * @return A REST client builder for use by chat clients.
     */
    @Bean
    public RestClient.Builder restClientBuilder() {
        RestClient.Builder builder = RestClient
                .builder()
                .requestFactory(ClientHttpRequestFactoryBuilder.simple().build())
                .requestInterceptor(new ClientAuthorizationInterceptor())
                .requestInterceptor(new ChatClientLoggingInterceptor());
        return builder;
    }

    /**
     * Creates a chat client bean for OpenAI
     * 
     * @return An OpenAI chat client
     */
    @Bean
    @Qualifier("openAiChatClient")
    public ChatClient openAiChatClient(RestClient.Builder restClientBuilder) {

        OpenAiApi openAiApi = new OpenAiApi(openAiBaseUrl, openAiApiKey, restClientBuilder, WebClient.builder());
        OpenAiChatOptions openAiChatOptions = OpenAiChatOptions.builder().model("gpt-4o").streamUsage(false).build();
        OpenAiChatModel openAiChatModel = new OpenAiChatModel(openAiApi, openAiChatOptions);
        ChatClient.Builder openAiClientBuilder = ChatClient.builder(openAiChatModel);
        openAiClientBuilder.defaultAdvisors(new ErrorHandlingAdvisor());
        return openAiClientBuilder.build();

    }

    /**
     * Creates a FilterRegistrationBean for an AuthorizationFilter.
     * 
     * @return A FilterRegistrationBean for an AuthorizationFilter.
     */
    @Bean
    public FilterRegistrationBean<AuthorizationFilter> filterRegistrationBean() {

        FilterRegistrationBean<AuthorizationFilter> registration = new FilterRegistrationBean<AuthorizationFilter>();
        registration.setFilter(new AuthorizationFilter());
        registration.addUrlPatterns("/*");
        registration.setName("tokenRequestFilter");
        return registration;

    }

}
