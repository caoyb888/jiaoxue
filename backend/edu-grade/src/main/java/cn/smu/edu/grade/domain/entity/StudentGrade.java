package cn.smu.edu.grade.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 学生综合成绩汇总（{@code student_grade}），成绩回传导出的数据源。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("student_grade")
public class StudentGrade {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long classId;
    private Long studentId;

    /** 综合总分（按权重计算，NULL=待计算）。 */
    private BigDecimal totalScore;

    private BigDecimal attendScore;
    private BigDecimal quizScore;
    private BigDecimal interactionScore;
    private BigDecimal examScore;

    /** 线下成绩（教师导入，NULL=未导入）。 */
    private BigDecimal offlineScore;

    /** 计算状态：0-未计算 1-已计算。 */
    private Integer calcStatus;

    private LocalDateTime updatedAt;
}
