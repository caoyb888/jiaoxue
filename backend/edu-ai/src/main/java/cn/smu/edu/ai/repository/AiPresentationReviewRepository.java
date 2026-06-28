package cn.smu.edu.ai.repository;

import cn.smu.edu.ai.domain.document.AiPresentationReview;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface AiPresentationReviewRepository extends MongoRepository<AiPresentationReview, String> {

    Optional<AiPresentationReview> findByLessonIdAndStudentId(Long lessonId, Long studentId);

    List<AiPresentationReview> findByLessonId(Long lessonId);
}
