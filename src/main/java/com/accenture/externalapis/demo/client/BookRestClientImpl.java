package com.accenture.externalapis.demo.client;

import com.accenture.externalapis.demo.config.ExternalServiceProperties;
import com.accenture.externalapis.demo.dto.BookApiResponse;
import com.accenture.externalapis.demo.dto.BookDto;
import org.springframework.stereotype.Component;
import org.springframework.web.client.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

import java.util.Arrays;
import java.util.List;


@Component
public class BookRestClientImpl implements BookRestClient {

    private final RestClient restClient;

    public BookRestClientImpl(RestClient.Builder builder, ExternalServiceProperties properties) {

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(3000);
        requestFactory.setReadTimeout(3000);



        this.restClient = builder
                .baseUrl(properties.baseUrl())
                .build();


    }


    @Override
    public BookDto getBook(Long id) {
        try {
            BookApiResponse response = restClient.get()
                    .uri("/books/{id}", id)
                    .retrieve()
                    .body(BookApiResponse.class);

            return toMapDto(response);

        } catch (HttpClientErrorException e) {
            throw new ClientException("Client error from external service: " + e.getStatusCode(), e);
        } catch (HttpServerErrorException e) {
            throw new ClientException("Server error from external service: " + e.getStatusCode(), e);
        } catch (ResourceAccessException e) {
            throw new ClientException("External service unreachable: " + e.getMessage(), e);
        } catch (RestClientException e) {
            throw new ClientException("Invalid response from external service: " + e.getMessage(), e);
        }
    }

    @Override
    public List<BookDto> getAllBooks() {
        try {
            BookApiResponse[] responses = restClient.get()
                    .uri("/books")
                    .retrieve()
                    .body(BookApiResponse[].class);

            return Arrays.stream(responses)
                    .map(this::toMapDto)
                    .toList();
        } catch (HttpClientErrorException e) {
            throw new ClientException("Client error from external service: " + e.getStatusCode(), e);
        } catch (HttpServerErrorException e) {
            throw new ClientException("Server error from external service: " + e.getStatusCode(), e);
        } catch (ResourceAccessException e) {
            throw new ClientException("External service unreachable: " + e.getMessage(), e);
        } catch (RestClientException e) {
            throw new ClientException("Invalid response from external service: " + e.getMessage(), e);
        }
    }
    private BookDto toMapDto(BookApiResponse response) {
        return new BookDto(
                response.title(),
                response.author(),
                response.genre(),
                response.price()
        );
    }
}




