package cn.smu.edu.ai.repository;

import cn.smu.edu.ai.domain.document.AiQuestionTask;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface AiQuestionTaskRepository extends MongoRepository<AiQuestionTask, String> {

    Optional<AiQuestionTask> findByTaskId(String taskId);
}
