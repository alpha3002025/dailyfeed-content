package click.dailyfeed.content.domain.comment.api;

import click.dailyfeed.code.domain.content.comment.dto.CommentDto;
import click.dailyfeed.code.domain.member.member.dto.MemberProfileDto;
import click.dailyfeed.code.global.web.code.ResponseSuccessCode;
import click.dailyfeed.code.global.web.response.DailyfeedServerResponse;
import click.dailyfeed.content.domain.comment.service.CommentService;
import click.dailyfeed.feign.config.web.annotation.AuthenticatedMemberProfileSummary;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/comments/replies")
@RestController
public class CommentReplyController {
    private final CommentService commentService;

    /// /comments/replies
    @PostMapping("")
    public DailyfeedServerResponse<CommentDto.Comment> createReply(
            @AuthenticatedMemberProfileSummary MemberProfileDto.Summary member,
            @RequestHeader("Authorization") String authorizationHeader,
            HttpServletResponse httpResponse,
            @Valid @RequestBody CommentDto.CreateCommentRequest request) {
        CommentDto.Comment result = commentService.createReply(member, authorizationHeader, request, httpResponse);
        return DailyfeedServerResponse.<CommentDto.Comment>builder()
                .status(HttpStatus.OK.value())
                .result(ResponseSuccessCode.SUCCESS)
                .data(result)
                .build();
    }
}
