package cn.smu.edu.exam.converter;

import cn.smu.edu.exam.domain.dto.QuestionCreateDTO;
import cn.smu.edu.exam.domain.dto.QuestionOptionDTO;
import cn.smu.edu.exam.domain.dto.QuestionUpdateDTO;
import cn.smu.edu.exam.domain.entity.Question;
import cn.smu.edu.exam.domain.entity.QuestionOption;
import cn.smu.edu.exam.domain.vo.QuestionOptionVO;
import cn.smu.edu.exam.domain.vo.QuestionVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface QuestionConverter {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "creatorId", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Question toEntity(QuestionCreateDTO dto);

    @Mapping(target = "options", ignore = true)
    QuestionVO toVO(Question entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "bankId", ignore = true)
    @Mapping(target = "type", ignore = true)
    @Mapping(target = "creatorId", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(QuestionUpdateDTO dto, @MappingTarget Question entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "questionId", ignore = true)
    QuestionOption toOptionEntity(QuestionOptionDTO dto);

    QuestionOptionVO toOptionVO(QuestionOption option);
}
