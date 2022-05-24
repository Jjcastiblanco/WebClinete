package com.web.client.demo.service;

import com.web.client.demo.model.User;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.Base64;
import java.util.Date;
import java.util.TimeZone;

@Service
@AllArgsConstructor
@Slf4j
public class UserService {
    private static final String USERS_URL_TEMPLATE = "/users/{id}";
    private static final String BROKEN_URL_TEMPLATE = "/broken-url/{id}";
    public static final int DELAY_MILLIS = 100;
    public static final int MAX_RETRY_ATTEMPTS = 3;
    private final WebClient webClient;

    public Mono<String> getUserByIdAsync(final String id) {
        return webClient
                .get()
                .uri("https://noccapi-stg.paymentez.com/pse/order/PSE-27758/")
                .header("Auth-Token",this.getToken("hola"))
                .retrieve()
                .bodyToMono(String.class);
    }

    public User getUserByIdSync(final String id) {
        return webClient
                .get()
                .uri(USERS_URL_TEMPLATE, id)
                .retrieve()
                .bodyToMono(User.class)
                .block();
    }

    public User getUserWithRetry(final String id) {
        return webClient
                .get()
                .uri(BROKEN_URL_TEMPLATE, id)
                .retrieve()
                .bodyToMono(User.class)
                .retryWhen(Retry.fixedDelay(MAX_RETRY_ATTEMPTS, Duration.ofMillis(DELAY_MILLIS)))
                .block();
    }

    public User getUserWithFallback(final String id) {
        return webClient
                .get()
                .uri(BROKEN_URL_TEMPLATE, id)
                .retrieve()
                .bodyToMono(User.class)
                .doOnError(error -> log.error("An error has occurred {}", error.getMessage()))
                .onErrorResume(error -> Mono.just(new User()))
                .block();
    }

    public User getUserWithErrorHandling(final String id) {
        return webClient
                .get()
                .uri(BROKEN_URL_TEMPLATE, id)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError,
                        error -> Mono.error(new RuntimeException("API not found")))
                .onStatus(HttpStatus::is5xxServerError,
                        error -> Mono.error(new RuntimeException("Server is not responding")))
                .bodyToMono(User.class)
                .block();
    }

    public String getToken(String event){
        String formatTimestamp;
        String sha256;
        String unixTimeStamp;
        String authTokenEncoded = "";

        try{

            formatTimestamp = this.getTimestamp();

            unixTimeStamp = "7Ab3RDm1A3H0YQiwfximRxdDn1k21g" + formatTimestamp;

            MessageDigest messageDigestd = MessageDigest.getInstance("SHA-256");
            messageDigestd.update(unixTimeStamp.getBytes(StandardCharsets.UTF_8));
            byte[] digest = messageDigestd.digest();
            sha256 = String.format("%064x",new BigInteger(1, digest));

            String authToken = "WINGOQA-COP-SERVER" + ";" + formatTimestamp + ";" + sha256;
            byte[] message = authToken.getBytes(StandardCharsets.UTF_8);
            authTokenEncoded = Base64.getEncoder().encodeToString(message);


            return authTokenEncoded;
        } catch (Exception e) {
            e.getMessage();
        }
        return authTokenEncoded;
    }

    private String getTimestamp(){
        Date date = new Date();
        String time;
        String timestamp = "";
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        try {

            dateFormat.setTimeZone(TimeZone.getTimeZone("America/Bogota"));
            String timeFormat = dateFormat.format(date.getTime());
            Date parceTime = dateFormat.parse(timeFormat);
            time = String.valueOf(parceTime.getTime());
            timestamp = time.substring(0,10);


            return timestamp;

        } catch (Exception e) {
            e.getMessage();
        }
        return null;
    }

}
