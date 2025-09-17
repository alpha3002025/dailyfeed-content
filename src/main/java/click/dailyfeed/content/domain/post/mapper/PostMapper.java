package click.dailyfeed.content.domain.post.mapper;

import click.dailyfeed.code.domain.content.post.dto.PostDto;
import click.dailyfeed.code.domain.member.member.dto.MemberDto;
import click.dailyfeed.code.domain.member.member.dto.MemberProfileDto;
import click.dailyfeed.content.domain.post.entity.Post;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PostMapper {
    PostMapper INSTANCE = Mappers.getMapper(PostMapper.class);

    @Mapping(target = "id", source = "post.id")
    @Mapping(target = "title", source = "post.title")
    @Mapping(target = "content", source = "post.content")
    @Mapping(target = "authorId", expression = "java(author.getMemberId())")
    @Mapping(target = "authorName", expression = "java(author.getMemberName())")
    @Mapping(target = "authorHandle", expression = "java(author.getMemberHandle())")
    @Mapping(target = "viewCount", source = "post.viewCount")
    @Mapping(target = "likeCount", source = "post.likeCount")
    @Mapping(target = "commentCount", constant = "0")
    @Mapping(target = "createdAt", source = "post.createdAt")
    @Mapping(target = "updatedAt", source = "post.updatedAt")
    PostDto.Post toPostDto(Post post, MemberProfileDto.Summary author);

    @Mapping(target = "id", source = "post.id")
    @Mapping(target = "title", source = "post.title")
    @Mapping(target = "content", source = "post.content")
    @Mapping(target = "authorId", expression = "java(author.getMemberId())")
    @Mapping(target = "authorName", expression = "java(author.getMemberName())")
    @Mapping(target = "authorHandle", expression = "java(author.getMemberHandle())")
    @Mapping(target = "viewCount", source = "post.viewCount")
    @Mapping(target = "likeCount", source = "post.likeCount")
    @Mapping(target = "commentCount", source = "commentCount")
    @Mapping(target = "createdAt", source = "post.createdAt")
    @Mapping(target = "updatedAt", source = "post.updatedAt")
    PostDto.Post toPostDto(Post post, MemberProfileDto.Summary author, Integer commentCount);
}
