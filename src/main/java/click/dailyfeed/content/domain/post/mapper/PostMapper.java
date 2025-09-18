package click.dailyfeed.content.domain.post.mapper;

import click.dailyfeed.code.domain.content.post.dto.PostDto;
import click.dailyfeed.code.domain.member.member.dto.MemberProfileDto;
import click.dailyfeed.content.domain.post.entity.Post;
import org.springframework.stereotype.Component;

@Component
public class PostMapper {
    public PostDto.Post toPostDto(Post post, MemberProfileDto.Summary author){
        return PostDto.Post.builder()
                .id(post.getId())
                .title(post.getTitle())
                .content(post.getContent())
                .authorId(author.getId())
                .authorName(author.getDisplayName())
                .authorHandle(author.getMemberHandle())
                .viewCount(post.getViewCount())
                .likeCount(post.getLikeCount())
                .commentCount(post.getCommentsCount())
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .build();
    }

    public PostDto.Post toPostDto(Post post, MemberProfileDto.Summary author, Integer commentCount){
        return PostDto.Post.builder()
                .id(post.getId())
                .title(post.getTitle())
                .content(post.getContent())
                .authorId(author.getId())
                .authorName(author.getDisplayName())
                .authorHandle(author.getMemberHandle())
                .viewCount(post.getViewCount())
                .likeCount(post.getLikeCount())
                .commentCount(commentCount)
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .build();
    }
}
