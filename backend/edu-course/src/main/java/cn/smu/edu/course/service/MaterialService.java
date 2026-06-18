package cn.smu.edu.course.service;

import cn.smu.edu.common.result.PageResult;
import cn.smu.edu.course.domain.dto.MaterialCompleteDTO;
import cn.smu.edu.course.domain.dto.MaterialUploadDTO;
import cn.smu.edu.course.domain.vo.MaterialCompleteVO;
import cn.smu.edu.course.domain.vo.MaterialListItemVO;
import cn.smu.edu.course.domain.vo.MaterialUploadVO;

public interface MaterialService {

    MaterialUploadVO applyUpload(Long teacherId, MaterialUploadDTO dto);

    MaterialCompleteVO completeUpload(Long teacherId, MaterialCompleteDTO dto);

    PageResult<MaterialListItemVO> listMaterials(Long teacherId, String keyword, int page, int size);
}
