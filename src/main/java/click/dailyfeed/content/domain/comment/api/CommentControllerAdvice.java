package click.dailyfeed.content.domain.comment.api;

import click.dailyfeed.code.domain.content.comment.exception.CommentException;
import click.dailyfeed.code.domain.member.key.exception.JwtKeyException;
import click.dailyfeed.code.domain.member.member.code.MemberHeaderCode;
import click.dailyfeed.code.domain.member.member.exception.MemberException;
import click.dailyfeed.code.domain.member.token.exception.KeyRefreshErrorException;
import click.dailyfeed.code.domain.member.token.exception.TokenRefreshNeededException;
import click.dailyfeed.code.global.jwt.exception.InvalidTokenException;
import click.dailyfeed.code.global.web.code.ResponseSuccessCode;
import click.dailyfeed.code.global.web.excecption.DailyfeedWebException;
import click.dailyfeed.code.global.web.response.DailyfeedErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice(basePackages = "click.dailyfeed.content.domain.comment.api")
public class CommentControllerAdvice {

    @ExceptionHandler(CommentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public DailyfeedErrorResponse handleCommentException(
            CommentException e,
            HttpServletRequest request) {

        log.warn("Comment exception occurred: {}, path: {}",
                e.getCommentExceptionCode().getReason(),
                request.getRequestURI());

        return DailyfeedErrorResponse.of(
                e.getCommentExceptionCode().getCode(),
                ResponseSuccessCode.FAIL,
                e.getCommentExceptionCode().getMessage(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(KeyRefreshErrorException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public DailyfeedErrorResponse handleKeyRefreshErrorException(KeyRefreshErrorException e, HttpServletRequest request, HttpServletResponse response) {
        return DailyfeedErrorResponse.of(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                ResponseSuccessCode.FAIL,
                e.getMessage(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(JwtKeyException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public DailyfeedErrorResponse handleJwtKeyException(JwtKeyException e, HttpServletRequest request, HttpServletResponse response) {
        response.setHeader(MemberHeaderCode.X_RELOGIN_REQUIRED.getHeaderKey(), "true");
        return DailyfeedErrorResponse.of(
                HttpStatus.UNAUTHORIZED.value(),
                ResponseSuccessCode.FAIL,
                e.getMessage(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(InvalidTokenException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public DailyfeedErrorResponse handleInvalidTokenException(InvalidTokenException e, HttpServletRequest request, HttpServletResponse response) {
        log.error("Invalid token error: {}", e.getMessage());
        response.setHeader(MemberHeaderCode.X_RELOGIN_REQUIRED.getHeaderKey(), "true");
        return DailyfeedErrorResponse.of(
                HttpStatus.UNAUTHORIZED.value(),
                ResponseSuccessCode.FAIL,
                e.getMessage(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(TokenRefreshNeededException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public DailyfeedErrorResponse handleTokenRefreshNeededException(TokenRefreshNeededException e, HttpServletRequest request) {
        return DailyfeedErrorResponse.of(
                HttpStatus.UNAUTHORIZED.value(),
                ResponseSuccessCode.FAIL,
                e.getTokenExceptionCode().getMessage(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(MemberException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public DailyfeedErrorResponse handleMemberException(
            MemberException e,
            HttpServletRequest request
    ) {
        log.warn("Member exception occurred: {}, path: {}",
                e.getMemberExceptionCode().getReason(),
                request.getRequestURI());

        return DailyfeedErrorResponse.of(
                e.getMemberExceptionCode().getCode(),
                ResponseSuccessCode.FAIL,
                e.getMemberExceptionCode().getMessage(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(DailyfeedWebException.class)
    public DailyfeedErrorResponse handleMemberException(
            DailyfeedWebException e,
            HttpServletRequest request
    ) {
        log.warn("Member exception occurred: {}, path: {}",
                e.getWebExceptionCode().getExceptionCode(),
                request.getRequestURI());

        return DailyfeedErrorResponse.of(
                e.getWebExceptionCode().getStatusCode(),
                ResponseSuccessCode.FAIL,
                e.getWebExceptionCode().getExceptionCode(),
                request.getRequestURI()
        );
    }

    // 일반적인 RuntimeException 처리 (예상치 못한 오류)
    @ExceptionHandler(RuntimeException.class)
    public DailyfeedErrorResponse handleRuntimeException(
            RuntimeException e,
            HttpServletRequest request) {

        log.error("Unexpected runtime exception occurred", e);

        return DailyfeedErrorResponse.of(
                500,
                ResponseSuccessCode.FAIL,
                "서버 내부 오류가 발생했습니다.",
                request.getRequestURI()
        );
    }

    @ExceptionHandler(Exception.class)
    public DailyfeedErrorResponse handleException(
            Exception e,
            HttpServletRequest request
    ){
        log.error("Unexpected exception occurred", e);

        return DailyfeedErrorResponse.of(
                500,
                ResponseSuccessCode.FAIL,
                "서버 내부 오류가 발생했습니다.",
                request.getRequestURI()
        );
    }
}
