package com.chimpchat.api.shared.web;

import com.chimpchat.api.auth.business.AuthException;
import com.chimpchat.api.channel.business.InstagramChannelBusiness;
import com.chimpchat.api.channel.business.TokenEncryptionBusiness;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class ApiExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(AuthException.class)
    ProblemDetail handleAuthException(AuthException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(exception.getStatus(), exception.getMessage());
        problem.setTitle("Authentication request failed");
        problem.setProperty("code", exception.getCode());
        return problem;
    }

    @ExceptionHandler(InstagramChannelBusiness.ChannelException.class)
    ProblemDetail handleChannelException(InstagramChannelBusiness.ChannelException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(exception.getStatus(), exception.getMessage());
        problem.setTitle("Channel request failed");
        problem.setProperty("code", exception.getCode());
        return problem;
    }

    @ExceptionHandler(TokenEncryptionBusiness.ChannelException.class)
    ProblemDetail handleTokenEncryptionException(TokenEncryptionBusiness.ChannelException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(exception.getStatus(), exception.getMessage());
        problem.setTitle("Channel credentials could not be stored");
        problem.setProperty("code", exception.getCode());
        return problem;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail handleValidationException(MethodArgumentNotValidException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "Please check the information you entered and try again.");
        problem.setTitle("Invalid request");
        problem.setProperty("code", "invalid_request");
        return problem;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ProblemDetail handleUnreadableRequest(HttpMessageNotReadableException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "We could not read that request. Please try again.");
        problem.setTitle("Invalid request");
        problem.setProperty("code", "invalid_request");
        return problem;
    }

    @ExceptionHandler(ResponseStatusException.class)
    ProblemDetail handleResponseStatusException(ResponseStatusException exception) {
        String detail = exception.getReason() == null
                ? "The request could not be completed."
                : exception.getReason();
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(exception.getStatusCode(), detail);
        problem.setTitle("Request failed");
        problem.setProperty("code", exception.getStatusCode().value() == 401
                ? "unauthorized" : "request_failed");
        return problem;
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail handleUnexpectedException(Exception exception) {
        logger.error("Unhandled API error", exception);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,
                "Something went wrong on our side. Please try again.");
        problem.setTitle("Request failed");
        problem.setProperty("code", "internal_error");
        return problem;
    }
}
