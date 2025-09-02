package com.example.apiintegration.service;

import com.example.common_lib.model.exception.ServiceUnavailableException1;
import com.example.common_lib.model.response.ApiResponse1;
import com.example.common_lib.model.response.ExchangeResponse;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

@Service
public class ReactiveCurrencyStreamService {

  private final WebClient webClient;
  private final String apiKey;
  private final AtomicReference<Double> latestRate = new AtomicReference<>(null);

  public ReactiveCurrencyStreamService(
      WebClient.Builder builder,
      @Value("${exchange.api.base-url}") String baseUrl,
      @Value("${exchange.api.key}") String apiKey) {
    this.webClient = builder.baseUrl(baseUrl).build();
    this.apiKey = apiKey;

    startRateStream(); // Flux automatique
  }

  /** Met à jour le taux XOF toutes les 30 secondes depuis CurrencyAPI.com */
  private void startRateStream() {
    Flux.interval(Duration.ofSeconds(30))
        .flatMap(
            tick ->
                fetchXafRate()
                    .doOnError(e -> System.err.println("Erreur API: " + e.getMessage()))
                    .onErrorResume(e -> Mono.empty()))
        .subscribe(rate -> latestRate.set(rate));
  }

  /** Récupère le taux XOF depuis CurrencyAPI.com */
  private Mono<Double> fetchXafRate() {
    return webClient
        .get()
        .uri(
            uriBuilder ->
                uriBuilder.queryParam("apikey", apiKey).queryParam("base_currency", "CAD").build())
        .retrieve()
        .bodyToMono(ExchangeResponse.class)
        .map(
            response -> {
              ExchangeResponse.CurrencyData xofData = response.getData().get("XOF");
              if (xofData == null || xofData.getValue() == null)
                throw new RuntimeException("Taux XOF introuvable");
              return xofData.getValue();
            })
        .retryWhen(Retry.backoff(3, Duration.ofSeconds(2)))
        .onErrorResume(
            WebClientResponseException.ServiceUnavailable.class,
            e -> {
              System.err.println("Service externe indisponible");
              return Mono.empty();
            });
  }

  /** Flux continu des taux XOF */
  public Flux<Double> rateStream() {
    return Flux.interval(Duration.ofSeconds(5))
        .map(tick -> latestRate.get())
        .filter(rate -> rate != null);
  }

  /** Flux continu des montants convertis CAD -> XOF */
  public Flux<BigDecimal> convertToXafStream(BigDecimal cadAmount) {
    return rateStream().map(rate -> cadAmount.multiply(BigDecimal.valueOf(rate)));
  }

  public Double getLatestRate() {
    return latestRate.get();
  }

  /** Conversion snapshot CAD -> XOF */
  public Mono<ApiResponse1<BigDecimal>> convertToXaf(BigDecimal cadAmount) {
    Double rate = latestRate.get();

    if (rate == null) {
      // On lève l'exception dans le flux Mono
      return Mono.error(
          new ServiceUnavailableException1(
              "Taux indisponible. Réessayez plus tard.", "/currenty/convert"));
    }

    BigDecimal result = cadAmount.multiply(BigDecimal.valueOf(rate));

    return Mono.just(ApiResponse1.success("Conversion réussie", result, HttpStatus.OK.value()));
  }
}
