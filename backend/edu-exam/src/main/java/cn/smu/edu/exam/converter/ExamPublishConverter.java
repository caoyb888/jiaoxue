package cn.smu.edu.exam.converter;

import cn.smu.edu.exam.domain.dto.ExamPublishCreateDTO;
import cn.smu.edu.exam.domain.dto.ExamPublishUpdateDTO;
import cn.smu.edu.exam.domain.entity.ExamPublish;
import cn.smu.edu.exam.domain.vo.ExamPublishVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface ExamPublishConverter {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "teacherId", ignore = true)
    @Mapping(target = "passwordHash", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    ExamPublish toEntity(ExamPublishCreateDTO dto);

    @Mapping(target = "hasPassword", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "statusLabel", ignore = true)
    ExamPublishVO toVO(ExamPublish entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "paperId", ignore = true)
    @Mapping(target = "classId", ignore = true)
    @Mapping(target = "teacherId", ignore = true)
    @Mapping(target = "passwordHash", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(ExamPublishUpdateDTO dto, @MappingTarget ExamPublish entity);
}
