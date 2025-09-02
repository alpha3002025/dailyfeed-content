package click.dailyfeed.content.domain.comment.mapper;

import click.dailyfeed.code.domain.content.comment.dto.CommentDto;
import click.dailyfeed.code.domain.member.member.dto.MemberDto;
import click.dailyfeed.code.global.web.response.DailyfeedPage;
import click.dailyfeed.content.domain.comment.entity.Comment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.springframework.data.domain.Page;

import java.util.List;

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
    @Mapping(target = "authorName", source = "author.name")
    @Mapping(target = "authorEmail", source = "author.email")
    @Mapping(target = "postId", source = "comment.post.id")
    @Mapping(target = "parentId", source = "comment.parent.id")
    @Mapping(target = "depth", source = "comment.depth")
    @Mapping(target = "children", source = "comment.children")
    @Mapping(target = "createdAt", source = "comment.createdAt")
    @Mapping(target = "updatedAt", source = "comment.updatedAt")
    CommentDto.Comment toComment(Comment comment, MemberDto.Member author);

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

    default <T> DailyfeedPage<T> fromJpaCommentPage(Page<Comment> page, List<T> content) {
        return DailyfeedPage.<T>builder()
                .content(content)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .isFirst(page.isFirst())
                .isLast(page.isLast())
                .hasNext(page.hasNext())
                .hasPrevious(page.hasPrevious())
                .build();
    }

    default <T> DailyfeedPage<T> emptyPage(){
        return DailyfeedPage.<T>builder()
                .content(List.of())
                .page(0)
                .size(0)
                .totalElements(0)
                .totalPages(0)
                .isFirst(true)
                .isLast(true)
                .hasNext(false)
                .hasPrevious(false)
                .build();
    }
}
