package cn.smu.edu.ai.repository;

import cn.smu.edu.ai.domain.document.AiMindmap;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface AiMindmapRepository extends MongoRepository<AiMindmap, String> {

    Optional<AiMindmap> findByLessonId(Long lessonId);
}
