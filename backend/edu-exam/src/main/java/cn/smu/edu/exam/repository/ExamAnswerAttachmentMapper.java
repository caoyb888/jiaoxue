package cn.smu.edu.exam.repository;

import cn.smu.edu.exam.domain.entity.ExamAnswerAttachment;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ExamAnswerAttachmentMapper extends BaseMapper<ExamAnswerAttachment> {

    @Select("SELECT * FROM exam_answer_attachment WHERE student_answer_id = #{studentAnswerId} ORDER BY sort_order ASC")
    List<ExamAnswerAttachment> selectByStudentAnswerId(@Param("studentAnswerId") Long studentAnswerId);
}
