package com.example.apiintegration.web;

import com.example.apiintegration.service.ReactiveCurrencyStreamService;
import java.math.BigDecimal;
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
  public Mono<BigDecimal> convert(@RequestParam BigDecimal amount) {
    return service.convertToXaf(amount);
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
