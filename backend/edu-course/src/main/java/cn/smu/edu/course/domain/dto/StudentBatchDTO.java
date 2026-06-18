package cn.smu.edu.course.domain.dto;

import lombok.Data;

import java.util.List;

@Data
public class StudentBatchDTO {

    private List<Long> studentIds;
    private List<String> studentNos;
}
