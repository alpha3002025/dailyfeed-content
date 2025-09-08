package click.dailyfeed.content.domain.post.mapper;

import click.dailyfeed.code.domain.content.post.dto.PostDto;
import click.dailyfeed.code.domain.member.member.dto.MemberDto;
import click.dailyfeed.code.global.web.response.DailyfeedPage;
import click.dailyfeed.content.domain.post.entity.Post;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;
import org.springframework.data.domain.Page;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PostMapper {
    PostMapper INSTANCE = Mappers.getMapper(PostMapper.class);

    @Mapping(target = "id", source = "post.id")
    @Mapping(target = "title", source = "post.title")
    @Mapping(target = "content", source = "post.content")
    @Mapping(target = "authorId", expression = "java(author.getId())")
    @Mapping(target = "authorName", expression = "java(author.getName())")
    @Mapping(target = "authorEmail", expression = "java(author.getEmail())")
    @Mapping(target = "viewCount", source = "post.viewCount")
    @Mapping(target = "likeCount", source = "post.likeCount")
    @Mapping(target = "commentCount", source = "commentCount")
    @Mapping(target = "createdAt", source = "post.createdAt")
    @Mapping(target = "updatedAt", source = "post.updatedAt")
    PostDto.Post toPostDto(Post post, MemberDto.Member author, Integer commentCount);

    @Mapping(target = "id", source = "post.id")
    @Mapping(target = "title", source = "post.title")
    @Mapping(target = "content", source = "post.content")
    @Mapping(target = "authorId", expression = "java(author.getId())")
    @Mapping(target = "authorName", expression = "java(author.getName())")
    @Mapping(target = "authorEmail", expression = "java(author.getEmail())")
    @Mapping(target = "viewCount", source = "post.viewCount")
    @Mapping(target = "likeCount", source = "post.likeCount")
    @Mapping(target = "commentCount", constant = "0")
    @Mapping(target = "createdAt", source = "post.createdAt")
    @Mapping(target = "updatedAt", source = "post.updatedAt")
    PostDto.Post toPostDto(Post post, MemberDto.Member author);

    @Mapping(target = "id", source = "post.id")
    @Mapping(target = "title", source = "post.title")
    @Mapping(target = "content", source = "post.content")
    @Mapping(target = "authorId", ignore = true)
    @Mapping(target = "authorName", ignore = true)
    @Mapping(target = "authorEmail", ignore = true)
    @Mapping(target = "viewCount", source = "post.viewCount")
    @Mapping(target = "likeCount", source = "post.likeCount")
    @Mapping(target = "commentCount", expression = "java(post.getComments().size())")
    @Mapping(target = "createdAt", source = "post.createdAt")
    @Mapping(target = "updatedAt", source = "post.updatedAt")
    PostDto.Post toPostDto(Post post);

//    @Mapping(target = "id", source = "post.id")
//    @Mapping(target = "title", source = "post.title")
//    @Mapping(target = "content", source = "post.content")
//    @Mapping(target = "authorId", source = "post.authorId")
//    @Mapping(target = "viewCount", source = "post.viewCount")
//    @Mapping(target = "likeCount", source = "post.likeCount")
//    @Mapping(target = "createdAt", source = "post.createdAt")
//    @Mapping(target = "updatedAt", source = "post.updatedAt")
//    @Mapping(target = "authorName", ignore = true) // Service에서 설정
//    @Mapping(target = "authorEmail", ignore = true) // Service에서 설정
//    @Mapping(target = "commentCount", ignore = true) // Service에서 설정
    PostDto.Post toPostDtoIgnoreAuthor(Post post);


    default <T> DailyfeedPage<T> fromPostPage(Page<T> page) {
        return DailyfeedPage.<T>builder()
                .content(page.getContent())
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

    default <T> DailyfeedPage<T> fromJpaPostPage(Page<Post> page, List<T> content) {
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
