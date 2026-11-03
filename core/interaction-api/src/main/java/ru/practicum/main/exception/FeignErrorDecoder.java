package ru.practicum.main.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Response;
import feign.codec.ErrorDecoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.practicum.stat.dto.ApiError;

import java.io.IOException;
import java.io.InputStream;

@Slf4j
@Component
public class FeignErrorDecoder implements ErrorDecoder {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Exception decode(String methodKey, Response response) {
        String message = extractMessage(response);
        return switch (response.status()) {
            case 400 -> new ValidationException(message);
            case 404 -> new NotFoundException(message);
            case 409 -> new ConflictException(message);
            default -> new ServiceUnavailableException(message);
        };
    }

    private String extractMessage(Response response) {
        String defaultMessage = "Internal service request failed with status " + response.status();
        if (response.body() == null) {
            return defaultMessage;
        }
        try (InputStream inputStream = response.body().asInputStream()) {
            ApiError apiError = objectMapper.readValue(inputStream, ApiError.class);
            if (apiError != null && apiError.getMessage() != null && !apiError.getMessage().isBlank()) {
                return apiError.getMessage();
            }
        } catch (IOException e) {
            log.warn("Не удалось распарсить тело ошибки от зависимого сервиса: {}", e.getMessage());
        }
        return defaultMessage;
    }
}
