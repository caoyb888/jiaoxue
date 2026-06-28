package cn.smu.edu.ai.domain.vo;

import cn.smu.edu.ai.domain.document.AiPresentationReview;

import java.util.List;

/**
 * 汇报点评视图（S8-05），供教师端雷达图/评语展示。
 */
public record PresentationReviewVO(
        Long lessonId,
        Long studentId,
        String studentName,
        Double totalScore,
        String overallComment,
        boolean parsed,
        List<DimensionVO> dimensions) {

    public record DimensionVO(String name, double weight, double maxScore, double score, String comment) {
    }

    public static PresentationReviewVO of(AiPresentationReview r) {
        List<DimensionVO> dims = r.getDimensions() == null ? List.of()
                : r.getDimensions().stream()
                .map(d -> new DimensionVO(d.getName(), d.getWeight(), d.getMaxScore(), d.getScore(), d.getComment()))
                .toList();
        return new PresentationReviewVO(
                r.getLessonId(), r.getStudentId(), r.getStudentName(),
                r.getTotalScore(), r.getOverallComment(), r.isParsed(), dims);
    }
}
