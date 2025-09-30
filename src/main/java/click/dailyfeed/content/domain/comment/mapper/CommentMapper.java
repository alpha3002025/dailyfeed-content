package click.dailyfeed.content.domain.comment.mapper;

import click.dailyfeed.code.domain.content.comment.dto.CommentDto;
import click.dailyfeed.code.domain.member.member.dto.MemberProfileDto;
import click.dailyfeed.content.domain.comment.entity.Comment;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

@Component
public class CommentMapper {

    public CommentDto.Comment toCommentNonRecursive(Comment comment){
        return CommentDto.Comment.builder()
                .id(comment.getId())
                .content(comment.getContent())
                .authorId(comment.getAuthorId())
                .postId(comment.getPost().getId())
                .parentId(comment.getParent() != null ? comment.getParent().getId() : null)
                .depth(comment.getDepth())
                .children(comment.getChildren().stream()
                        .map(this::toCommentNonRecursive)
                        .toList())
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .build();
    }

    // 순환 참조를 방지하기 위한 오버로드된 메서드
    public CommentDto.Comment toCommentWith(Comment comment, boolean includeChildren) {
        CommentDto.Comment.CommentBuilder builder = CommentDto.Comment.builder()
                .id(comment.getId())
                .content(comment.getContent())
                .authorId(comment.getAuthorId())
                .postId(comment.getPost().getId())
                .parentId(comment.getParent() != null ? comment.getParent().getId() : null)
                .depth(comment.getDepth())
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt());

        if (includeChildren) {
            builder.children(comment.getChildren().stream()
                    .map(child -> toCommentWith(child, true))
                    .toList());
        } else {
            builder.children(new ArrayList<>());
        }

        return builder.build();
    }


    public CommentDto.Comment toCommentNonRecursive(Comment comment, MemberProfileDto.Summary author){
        return CommentDto.Comment.builder()
                .id(comment.getId())
                .content(comment.getContent())
                .authorId(comment.getAuthorId())
                .authorName(author.getDisplayName())
                .authorHandle(author.getMemberHandle())
                .authorAvatarUrl(author.getAvatarUrl())
                .postId(comment.getPost().getId())
                .parentId(comment.getParent() != null ? comment.getParent().getId() : null)
                .depth(comment.getDepth())
                .children(comment.getChildren().stream()
                        .map(this::toCommentNonRecursive)
                        .toList())
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .build();
    }

    public CommentDto.CommentSummary toCommentSummary(Comment comment){
        return CommentDto.CommentSummary.builder()
                .id(comment.getId())
                .content(comment.getContent())
                .authorId(comment.getAuthorId())
                .postId(comment.getPost().getId())
                .parentId(comment.getParent() != null ? comment.getParent().getId() : null)
                .depth(comment.getDepth())
                .likeCount(comment.getLikeCount())
                .childrenCount(comment.getChildren().size())
                .createdAt(comment.getCreatedAt())
                .build();
    }
}
