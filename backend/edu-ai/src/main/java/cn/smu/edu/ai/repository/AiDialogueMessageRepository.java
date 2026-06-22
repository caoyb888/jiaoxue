package cn.smu.edu.ai.repository;

import cn.smu.edu.ai.domain.document.AiDialogueMessage;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface AiDialogueMessageRepository extends MongoRepository<AiDialogueMessage, String> {

    List<AiDialogueMessage> findBySessionIdOrderBySeqAsc(String sessionId);

    long countBySessionId(String sessionId);
}
