package com.accenture.externalapis.demo.client;

import com.accenture.externalapis.demo.config.ExternalServiceProperties;
import com.accenture.externalapis.demo.dto.BookApiResponse;
import com.accenture.externalapis.demo.dto.BookDto;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import org.springframework.core.codec.DecodingException;
import java.time.Duration;
import reactor.util.retry.Retry;
import java.util.List;

@Component
public class BookWebClientImpl implements BookWebClient {

    private WebClient webClient;

    public BookWebClientImpl(WebClient.Builder builder, ExternalServiceProperties properties) {

        this.webClient = builder
                .baseUrl(properties.baseUrl())
                .build();
    }

    private BookDto mapToBookDto(BookApiResponse response) {
        return new BookDto(
                response.title(),
                response.author(),
                response.genre(),
                response.price()
        );
    }

    private <T> Mono<T> handleMonoErrors(Mono<T> mono) {
        return mono
                .onErrorMap(WebClientResponseException.class, ex ->
                        new ClientException("External service returned error " + ex.getStatusCode(), ex))
                .onErrorMap(WebClientRequestException.class, ex ->
                        new ClientException("External service unreachable: " + ex.getMessage(), ex))
                .onErrorMap(DecodingException.class, ex ->
                        new ClientException("Invalid response from external service ", ex));

    }

    private <T> Flux<T> handleFluxErrors(Flux<T> flux) {
        return flux
                .onErrorMap(WebClientResponseException.class, ex ->
                        new ClientException("External service returned error " + ex.getStatusCode(), ex))
                .onErrorMap(WebClientRequestException.class, ex ->
                        new ClientException("External service unreachable: " + ex.getMessage(), ex))
                .onErrorMap(DecodingException.class, ex ->
                        new ClientException("Invalid response from external service ", ex));
    }


    @Override
    public Mono<BookDto> getBookAsync(Long id) {
        return handleMonoErrors(
                webClient.get()
                        .uri("/books/{id}", id)
                        .retrieve()
                        .bodyToMono(BookApiResponse.class)
                        .timeout(Duration.ofSeconds(5))
                        .retryWhen(Retry
                                .fixedDelay(2, Duration.ofSeconds(1))
                                .filter(ex -> ex instanceof WebClientResponseException w && w.getStatusCode().is5xxServerError()))
                        .map(this::mapToBookDto)
        );
    }


    @Override
    public Flux<BookDto> getAllBooksAsync() {
        return handleFluxErrors(
                webClient.get()
                        .uri("/books")
                        .retrieve()
                        .bodyToFlux(BookApiResponse.class)
                        .timeout(Duration.ofSeconds(5))
                        .map(this::mapToBookDto)
        );
    }


    @Override
    public Mono<List<BookDto>> getBooksInParallel(Long id1, Long id2) {
        Mono<BookDto> book1 = getBookAsync(id1);
        Mono<BookDto> book2 = getBookAsync(id2);
        return Mono.zip(book1, book2)
                .map(tuple -> List.of(tuple.getT1(), tuple.getT2()));
    }
}

