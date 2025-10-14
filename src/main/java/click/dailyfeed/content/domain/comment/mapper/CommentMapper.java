package click.dailyfeed.content.domain.comment.mapper;

import click.dailyfeed.code.domain.content.comment.dto.CommentDto;
import click.dailyfeed.code.domain.member.member.dto.MemberProfileDto;
import click.dailyfeed.code.global.menu.MessageProperties;
import click.dailyfeed.content.domain.comment.entity.Comment;
import org.springframework.stereotype.Component;

@Component
public class CommentMapper {
    public CommentDto.Comment fromCommentNonRecursive(Comment comment, MemberProfileDto.Summary author){
        return CommentDto.Comment.builder()
                .id(comment.getId())
                .content(comment.getContent())
                .authorId(comment.getAuthorId())
                .authorName(author != null ? author.getDisplayName() : MessageProperties.KO.DELETED_USER)
                .authorHandle(author != null ? author.getMemberHandle() : MessageProperties.KO.DELETED_HANDLE)
                .authorAvatarUrl(author != null ? author.getAvatarUrl() : MessageProperties.KO.NO_AVATAR_URL)
                .postId(comment.getPost().getId())
                .parentId(comment.getParent() != null ? comment.getParent().getId() : null)
                .depth(comment.getDepth())
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .build();
    }
}
