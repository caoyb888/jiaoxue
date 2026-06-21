package cn.smu.edu.ai.repository;

import cn.smu.edu.ai.domain.document.AiLectureTranscript;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface AiLectureTranscriptRepository extends MongoRepository<AiLectureTranscript, String> {

    Optional<AiLectureTranscript> findByLessonId(Long lessonId);
}
