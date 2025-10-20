package click.dailyfeed.content.domain.deadletter.mapper;

import click.dailyfeed.code.domain.activity.dto.MemberActivityDto;
import click.dailyfeed.content.domain.deadletter.document.FeignDeadLetterDocument;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class FeignDeadLetterMapper {
    @Qualifier("feignObjectMapper")
    private final ObjectMapper objectMapper;

    public FeignDeadLetterDocument fromPostRequest(MemberActivityDto.PostActivityRequest request) throws JsonProcessingException {
        String strPayload = objectMapper.writeValueAsString(request);
        return FeignDeadLetterDocument.newDeadLetter(strPayload);
    }

    public FeignDeadLetterDocument fromCommentRequest(MemberActivityDto.CommentActivityRequest request) throws JsonProcessingException {
        String strPayload = objectMapper.writeValueAsString(request);
        return FeignDeadLetterDocument.newDeadLetter(strPayload);
    }

    public FeignDeadLetterDocument fromPostLikeRequest(MemberActivityDto.PostLikeActivityRequest request) throws JsonProcessingException {
        String strPayload = objectMapper.writeValueAsString(request);
        return FeignDeadLetterDocument.newDeadLetter(strPayload);
    }

    public FeignDeadLetterDocument fromCommentLikeRequest(MemberActivityDto.CommentLikeActivityRequest request) throws JsonProcessingException {
        String strPayload = objectMapper.writeValueAsString(request);
        return FeignDeadLetterDocument.newDeadLetter(strPayload);
    }
}
