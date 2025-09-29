package click.dailyfeed.content.domain.post.repository.mongo;

import click.dailyfeed.content.domain.post.document.PostLikeDocument;
import click.dailyfeed.content.domain.post.projection.PostLikeCountProjection;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;
import java.util.Set;

public interface PostLikeMongoRepository extends MongoRepository<PostLikeDocument, ObjectId> {

    // 전체 좋아요 도큐먼트 수 카운트
//    @Query(value = "{}", count = true)
//    long countAllPostLikes();

    // 특정 포스트의 좋아요 수 카운트
    @Query(value = "{ 'post_pk': ?0 }", count = true)
    long countByPostPk(Long postPk);

    // 특정 회원이 누른 좋아요 수 카운트
    @Query(value = "{ 'member_id': ?0 }", count = true)
    long countByMemberId(Long memberId);

    // 여러 포스트에 대한 좋아요 수를 한 번에 조회
    @Aggregation(pipeline = {
            "{ '$match': { 'post_pk': { '$in': ?0 } } }",
            "{ '$group': { '_id': '$post_pk', 'count': { '$sum': 1 } } }",
            "{ '$project': { 'postPk': '$_id', 'likeCount': '$count', '_id': 0 } }"
    })
    List<PostLikeCountProjection> countLikesByPostPks(Set<Long> postPks);
}
