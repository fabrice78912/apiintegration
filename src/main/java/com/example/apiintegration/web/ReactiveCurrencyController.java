package com.example.apiintegration.web;

import com.example.apiintegration.service.ReactiveCurrencyStreamService;
import com.example.common_lib.model.exception.ServiceUnavailableException1;
import com.example.common_lib.model.response.ApiResponse1;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/currenty")
public class ReactiveCurrencyController {

  private final ReactiveCurrencyStreamService service;

  public ReactiveCurrencyController(ReactiveCurrencyStreamService service) {
    this.service = service;
  }

  // Snapshot (un seul résultat)
  @GetMapping("/convert")
  public Mono<ApiResponse1<BigDecimal>> convert(
      @RequestParam(required = true) @NotNull @DecimalMin("0.01") BigDecimal amount) {
    return service
        .convertToXaf(amount)
        .onErrorResume(
            ServiceUnavailableException1.class,
            ex ->
                Mono.just(
                    ApiResponse1.error(
                        ex.getMessage(), ex.getCode(), HttpStatus.SERVICE_UNAVAILABLE.value())));
  }

  // Flux continu (montant en temps réel)
  @GetMapping(value = "/convert/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public Flux<BigDecimal> convertStream(@RequestParam BigDecimal amount) {
    return service.convertToXafStream(amount).take(10);
  }

  // Flux des taux uniquement
  @GetMapping(value = "/rates/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public Flux<Double> ratesStream() {
    return service.rateStream();
  }
}
