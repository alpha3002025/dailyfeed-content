package click.dailyfeed.content.domain.deadletter.service;

import click.dailyfeed.code.domain.activity.dto.MemberActivityDto;
import click.dailyfeed.code.global.feign.exception.FeignApiSerializationFailException;
import click.dailyfeed.content.domain.deadletter.document.FeignDeadLetterDocument;
import click.dailyfeed.content.domain.deadletter.mapper.FeignDeadLetterMapper;
import click.dailyfeed.content.domain.deadletter.mongo.FeignDeadLetterRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
@Transactional
@Service
public class FeignDeadLetterService {
    private final FeignDeadLetterRepository deadLetterRepository;
    private final FeignDeadLetterMapper feignDeadLetterMapper;

    public void createPostActivityDeadLetter(MemberActivityDto.PostActivityRequest request){
        try{
            FeignDeadLetterDocument document = feignDeadLetterMapper.fromPostRequest(request);
            deadLetterRepository.save(document);
        }catch (JsonProcessingException e){
            throw new FeignApiSerializationFailException();
        }
    }

    public void createCommentActivityDeadLetter(MemberActivityDto.CommentActivityRequest request){
        try{
            FeignDeadLetterDocument document = feignDeadLetterMapper.fromCommentRequest(request);
            deadLetterRepository.save(document);
        }catch (JsonProcessingException e){
            throw new FeignApiSerializationFailException();
        }
    }


    public void createPostLikeActivityDeadLetter(MemberActivityDto.PostLikeActivityRequest request){
        try{
            FeignDeadLetterDocument document = feignDeadLetterMapper.fromPostLikeRequest(request);
            deadLetterRepository.save(document);
        }catch (JsonProcessingException e){
            throw new FeignApiSerializationFailException();
        }
    }

    public void createCommentLikeActivityDeadLetter(MemberActivityDto.CommentLikeActivityRequest request){
        try{
            FeignDeadLetterDocument document = feignDeadLetterMapper.fromCommentLikeRequest(request);
            deadLetterRepository.save(document);
        }catch (JsonProcessingException e){
            throw new FeignApiSerializationFailException();
        }
    }
}
