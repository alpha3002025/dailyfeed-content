package click.dailyfeed.content.domain.comment.mapper;

import click.dailyfeed.code.domain.content.comment.dto.CommentDto;
import click.dailyfeed.code.domain.member.member.dto.MemberProfileDto;
import click.dailyfeed.content.domain.comment.entity.Comment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CommentMapper {
    @Mapping(target = "id", source = "comment.id")
    @Mapping(target = "content", source = "comment.content")
    @Mapping(target = "authorId", source = "comment.authorId")
    @Mapping(target = "postId", source = "comment.post.id")
    @Mapping(target = "parentId", source = "comment.parent.id")
    @Mapping(target = "depth", source = "comment.depth")
    @Mapping(target = "children", source = "comment.children")
    @Mapping(target = "createdAt", source = "comment.createdAt")
    @Mapping(target = "updatedAt", source = "comment.updatedAt")
    CommentDto.Comment toComment(Comment comment);

    @Mapping(target = "id", source = "comment.id")
    @Mapping(target = "content", source = "comment.content")
    @Mapping(target = "authorId", source = "comment.authorId")
    @Mapping(target = "authorName", source = "author.memberName")
    @Mapping(target = "authorHandle", source = "author.memberHandle")
    @Mapping(target = "postId", source = "comment.post.id")
    @Mapping(target = "parentId", source = "comment.parent.id")
    @Mapping(target = "depth", source = "comment.depth")
    @Mapping(target = "children", source = "comment.children")
    @Mapping(target = "createdAt", source = "comment.createdAt")
    @Mapping(target = "updatedAt", source = "comment.updatedAt")
    CommentDto.Comment toComment(Comment comment, MemberProfileDto.Summary author);

    @Mapping(target = "id", source = "comment.id")
    @Mapping(target = "content", source = "comment.content")
    @Mapping(target = "authorId", source = "comment.authorId")
    @Mapping(target = "postId", source = "comment.post.id")
    @Mapping(target = "parentId", source = "comment.parent.id")
    @Mapping(target = "depth", source = "comment.depth")
    @Mapping(target = "likeCount", source = "comment.likeCount")
    @Mapping(target = "childrenCount", expression = "java(comment.getChildren().size())")
    @Mapping(target = "createdAt", source = "comment.createdAt")
    CommentDto.CommentSummary toCommentSummary(Comment comment);
}
