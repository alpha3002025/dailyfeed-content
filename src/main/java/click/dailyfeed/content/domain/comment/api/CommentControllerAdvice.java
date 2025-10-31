package click.dailyfeed.content.domain.comment.api;

import click.dailyfeed.code.domain.content.comment.exception.CommentException;
import click.dailyfeed.code.domain.member.member.exception.MemberException;
import click.dailyfeed.code.global.web.code.ResponseSuccessCode;
import click.dailyfeed.code.global.web.excecption.DailyfeedWebException;
import click.dailyfeed.code.global.web.response.DailyfeedErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice(basePackages = "click.dailyfeed.content.domain.comment.api")
public class CommentControllerAdvice {

    @ExceptionHandler(CommentException.class)
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

    @ExceptionHandler(MemberException.class)
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
