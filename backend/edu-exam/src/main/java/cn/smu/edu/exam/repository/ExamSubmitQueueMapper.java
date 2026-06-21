package cn.smu.edu.exam.repository;

import cn.smu.edu.exam.domain.entity.ExamSubmitQueue;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ExamSubmitQueueMapper extends BaseMapper<ExamSubmitQueue> {

    @Select("SELECT * FROM exam_submit_queue WHERE publish_id = #{publishId} AND student_id = #{studentId} LIMIT 1")
    ExamSubmitQueue selectByPublishAndStudent(@Param("publishId") Long publishId,
                                              @Param("studentId") Long studentId);

    @Select("SELECT * FROM exam_submit_queue WHERE process_status = 0 ORDER BY created_at ASC LIMIT #{limit}")
    List<ExamSubmitQueue> selectPending(@Param("limit") int limit);
}
