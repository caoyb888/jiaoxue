package cn.smu.edu.user.service.impl;

import cn.smu.edu.common.exception.BizException;
import cn.smu.edu.common.result.ErrorCode;
import cn.smu.edu.user.domain.entity.SysDept;
import cn.smu.edu.user.domain.vo.DeptVO;
import cn.smu.edu.user.repository.SysDeptMapper;
import cn.smu.edu.user.service.DeptService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DeptServiceImpl implements DeptService {

    private final SysDeptMapper deptMapper;

    @Override
    public List<DeptVO> getDeptTree() {
        List<SysDept> allDepts = deptMapper.selectList(
                new LambdaQueryWrapper<SysDept>()
                        .eq(SysDept::getIsDeleted, 0)
                        .orderByAsc(SysDept::getSortOrder));

        List<DeptVO> voList = allDepts.stream()
                .map(this::toVO)
                .collect(Collectors.toList());

        Map<Long, DeptVO> voMap = voList.stream()
                .collect(Collectors.toMap(DeptVO::getId, v -> v));

        List<DeptVO> roots = new ArrayList<>();
        for (DeptVO vo : voList) {
            if (vo.getParentId() == null) {
                roots.add(vo);
            } else {
                DeptVO parent = voMap.get(vo.getParentId());
                if (parent != null) {
                    if (parent.getChildren() == null) {
                        parent.setChildren(new ArrayList<>());
                    }
                    parent.getChildren().add(vo);
                }
            }
        }
        return roots;
    }

    @Override
    public DeptVO getDeptById(Long deptId) {
        SysDept dept = deptMapper.selectById(deptId);
        if (dept == null || dept.getIsDeleted() == 1) {
            throw new BizException(ErrorCode.PARAM_ERROR);
        }
        return toVO(dept);
    }

    private DeptVO toVO(SysDept dept) {
        DeptVO vo = new DeptVO();
        vo.setId(dept.getId());
        vo.setDeptCode(dept.getDeptCode());
        vo.setDeptName(dept.getDeptName());
        vo.setParentId(dept.getParentId());
        vo.setDeptType(dept.getDeptType());
        vo.setLevel(dept.getLevel());
        vo.setSortOrder(dept.getSortOrder());
        return vo;
    }
}
