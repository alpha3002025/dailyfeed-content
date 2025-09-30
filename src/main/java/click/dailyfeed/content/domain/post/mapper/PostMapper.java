package click.dailyfeed.content.domain.post.mapper;

import click.dailyfeed.code.domain.content.post.dto.PostDto;
import click.dailyfeed.code.domain.member.member.dto.MemberProfileDto;
import click.dailyfeed.content.domain.post.entity.Post;
import org.springframework.stereotype.Component;

@Component
public class PostMapper {
    public PostDto.Post fromCreatedPost(Post post, MemberProfileDto.Summary author){
        return PostDto.Post.builder()
                .id(post.getId())
                .title(post.getTitle())
                .content(post.getContent())
                .authorId(author != null ? author.getId() : null)
                .authorName(author != null ? author.getDisplayName() : null)
                .authorHandle(author != null ? author.getMemberHandle() : null)
                .authorAvatarUrl(author != null ? author.getAvatarUrl() : null)
                .viewCount(post.getViewCount())
                .likeCount(0L)
                .commentCount(0L)
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .build();
    }

    public PostDto.Post toPostDto(Post post, MemberProfileDto.Summary author, PostDto.PostLikeCountStatistics postLikeStatistics, PostDto.PostCommentCountStatistics commentCountStatistics) {
        return PostDto.Post.builder()
                .id(post.getId())
                .title(post.getTitle())
                .content(post.getContent())
                .authorId(author != null ? author.getId() : null)
                .authorName(author != null ? author.getDisplayName() : null)
                .authorHandle(author != null ? author.getMemberHandle() : null)
                .authorAvatarUrl(author != null ? author.getAvatarUrl() : null)
                .viewCount(post.getViewCount())
                .likeCount(postLikeStatistics != null ? postLikeStatistics.getLikeCount() : 0L)
                .commentCount(commentCountStatistics != null ? commentCountStatistics.getCommentCount() : 0L)
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .build();
    }

//    public PostDto.Post toPostDto(Post post, MemberProfileDto.Summary author, Long commentCount){
//        return PostDto.Post.builder()
//                .id(post.getId())
//                .title(post.getTitle())
//                .content(post.getContent())
//                .authorId(author.getId())
//                .authorName(author.getDisplayName())
//                .authorHandle(author.getMemberHandle())
//                .viewCount(post.getViewCount())
//                .likeCount(post.getLikeCount())
//                .commentCount(commentCount)
//                .createdAt(post.getCreatedAt())
//                .updatedAt(post.getUpdatedAt())
//                .build();
//    }

}
