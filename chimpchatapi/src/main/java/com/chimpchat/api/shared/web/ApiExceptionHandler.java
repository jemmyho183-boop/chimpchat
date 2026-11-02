package com.chimpchat.api.shared.web;

import com.chimpchat.api.auth.business.AuthException;
import com.chimpchat.api.channel.business.InstagramChannelBusiness;
import com.chimpchat.api.channel.business.TokenEncryptionBusiness;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {
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
                "Please provide a valid request.");
        problem.setTitle("Invalid request");
        problem.setProperty("code", "invalid_request");
        return problem;
    }
}
