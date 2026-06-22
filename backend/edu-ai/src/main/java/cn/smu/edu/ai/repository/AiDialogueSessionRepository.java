package cn.smu.edu.ai.repository;

import cn.smu.edu.ai.domain.document.AiDialogueSession;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface AiDialogueSessionRepository extends MongoRepository<AiDialogueSession, String> {

    Optional<AiDialogueSession> findBySessionId(String sessionId);
}
