package cn.smu.edu.ai.repository;

import cn.smu.edu.ai.domain.document.AiGroupDiscussion;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface AiGroupDiscussionRepository extends MongoRepository<AiGroupDiscussion, String> {

    Optional<AiGroupDiscussion> findByLessonIdAndGroupId(Long lessonId, Long groupId);

    List<AiGroupDiscussion> findByLessonId(Long lessonId);
}
