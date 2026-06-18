package cn.smu.edu.course.event;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaterialConvertEvent implements Serializable {

    private Long materialId;
    private Long teacherId;
    private String originalPath;
    private String fileType;
    private String bucketName;
    private String slideDir;
}
