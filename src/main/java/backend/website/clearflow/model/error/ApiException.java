package backend.website.clearflow.model.error;

import org.springframework.http.HttpStatus;
import org.springframework.web.ErrorResponseException;

public class ApiException extends ErrorResponseException {

    public ApiException(HttpStatus status, String message) {
        super(status, asProblemDetail(status, message), null);
    }

    private static org.springframework.http.ProblemDetail asProblemDetail(HttpStatus status, String message) {
        org.springframework.http.ProblemDetail detail = org.springframework.http.ProblemDetail.forStatusAndDetail(status, message);
        detail.setTitle(status.getReasonPhrase());
        return detail;
    }
}
