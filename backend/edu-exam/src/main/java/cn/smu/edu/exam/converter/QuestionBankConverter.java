package cn.smu.edu.exam.converter;

import cn.smu.edu.exam.domain.dto.QuestionBankCreateDTO;
import cn.smu.edu.exam.domain.entity.QuestionBank;
import cn.smu.edu.exam.domain.vo.QuestionBankVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface QuestionBankConverter {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "teacherId", ignore = true)
    @Mapping(target = "deptId", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    QuestionBank toEntity(QuestionBankCreateDTO dto);

    @Mapping(target = "editable", ignore = true)
    QuestionBankVO toVO(QuestionBank entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "teacherId", ignore = true)
    @Mapping(target = "deptId", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(cn.smu.edu.exam.domain.dto.QuestionBankUpdateDTO dto, @MappingTarget QuestionBank entity);
}
