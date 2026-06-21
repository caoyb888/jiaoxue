package cn.smu.edu.ai.repository;

import cn.smu.edu.ai.domain.document.AiReviewResult;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface AiReviewResultRepository extends MongoRepository<AiReviewResult, String> {

    List<AiReviewResult> findByPublishId(Long publishId);

    Optional<AiReviewResult> findByAnswerId(Long answerId);
}
