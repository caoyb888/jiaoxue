package cn.smu.edu.user.service;

import cn.smu.edu.user.domain.vo.DeptVO;
import java.util.List;

public interface DeptService {

    List<DeptVO> getDeptTree();

    DeptVO getDeptById(Long deptId);
}
