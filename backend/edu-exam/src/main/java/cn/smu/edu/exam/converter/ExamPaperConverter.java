package cn.smu.edu.exam.converter;

import cn.smu.edu.exam.domain.dto.ExamPaperCreateDTO;
import cn.smu.edu.exam.domain.dto.ExamPaperUpdateDTO;
import cn.smu.edu.exam.domain.entity.ExamPaper;
import cn.smu.edu.exam.domain.entity.ExamPaperQuestion;
import cn.smu.edu.exam.domain.vo.ExamPaperVO;
import cn.smu.edu.exam.domain.vo.PaperQuestionVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface ExamPaperConverter {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "creatorId", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    ExamPaper toEntity(ExamPaperCreateDTO dto);

    @Mapping(target = "editable", ignore = true)
    ExamPaperVO toVO(ExamPaper entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "creatorId", ignore = true)
    @Mapping(target = "isRandom", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(ExamPaperUpdateDTO dto, @MappingTarget ExamPaper entity);

    @Mapping(target = "question", ignore = true)
    PaperQuestionVO toPaperQuestionVO(ExamPaperQuestion relation);
}
