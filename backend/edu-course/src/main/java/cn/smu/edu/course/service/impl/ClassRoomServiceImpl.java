package cn.smu.edu.course.service.impl;

import cn.smu.edu.common.exception.BizException;
import cn.smu.edu.common.result.ErrorCode;
import cn.smu.edu.course.domain.dto.ClassRoomCreateDTO;
import cn.smu.edu.course.domain.dto.StudentBatchDTO;
import cn.smu.edu.course.domain.entity.ClassRoom;
import cn.smu.edu.course.domain.entity.ClassStudent;
import cn.smu.edu.course.domain.vo.ClassRoomDetailVO;
import cn.smu.edu.course.domain.vo.ClassRoomVO;
import cn.smu.edu.course.repository.ClassRoomMapper;
import cn.smu.edu.course.repository.ClassStudentMapper;
import cn.smu.edu.course.service.ClassRoomService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClassRoomServiceImpl implements ClassRoomService {

    private final ClassRoomMapper classRoomMapper;
    private final ClassStudentMapper classStudentMapper;

    @Override
    public List<ClassRoomVO> listMyClasses(Long userId, Integer userType, String semester, Integer status) {
        return classRoomMapper.selectMyClasses(userId, userType, semester, status);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createClassRoom(Long teacherId, Long deptId, ClassRoomCreateDTO dto) {
        long count = classRoomMapper.selectCount(new LambdaQueryWrapper<ClassRoom>()
                .eq(ClassRoom::getClassCode, dto.getClassCode())
                .eq(ClassRoom::getIsDeleted, 0));
        if (count > 0) {
            throw new BizException(ErrorCode.PARAM_ERROR.getCode(), "教学班编码已存在: " + dto.getClassCode());
        }

        ClassRoom classRoom = new ClassRoom();
        classRoom.setCourseId(dto.getCourseId());
        classRoom.setTeacherId(teacherId);
        classRoom.setClassName(dto.getClassName());
        classRoom.setClassCode(dto.getClassCode());
        classRoom.setSemester(dto.getSemester());
        classRoom.setDeptId(dto.getDeptId() != null ? dto.getDeptId() : deptId);
        classRoom.setStatus(1);
        classRoom.setStudentCount(0);
        classRoomMapper.insert(classRoom);

        log.info("教学班创建成功: classId={}, classCode={}, teacherId={}", classRoom.getId(), classRoom.getClassCode(), teacherId);
        return classRoom.getId();
    }

    @Override
    public ClassRoomDetailVO getClassDetail(Long classId) {
        ClassRoomDetailVO vo = classRoomMapper.selectClassDetail(classId);
        if (vo == null) {
            throw new BizException(ErrorCode.CLASS_NOT_FOUND);
        }
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addStudents(Long classId, StudentBatchDTO dto) {
        if (CollectionUtils.isEmpty(dto.getStudentIds())) {
            throw new BizException(ErrorCode.PARAM_ERROR.getCode(), "学生ID列表不能为空");
        }

        List<ClassStudent> toInsert = new ArrayList<>();
        for (Long studentId : dto.getStudentIds()) {
            long exists = classStudentMapper.selectCount(new LambdaQueryWrapper<ClassStudent>()
                    .eq(ClassStudent::getClassId, classId)
                    .eq(ClassStudent::getStudentId, studentId)
                    .eq(ClassStudent::getStatus, 1));
            if (exists > 0) {
                continue;
            }
            ClassStudent cs = new ClassStudent();
            cs.setClassId(classId);
            cs.setStudentId(studentId);
            cs.setStudentNo("");
            cs.setStatus(1);
            toInsert.add(cs);
        }

        if (!toInsert.isEmpty()) {
            toInsert.forEach(classStudentMapper::insert);
            // 更新班级人数冗余字段
            int count = classStudentMapper.countByClassId(classId);
            ClassRoom room = classRoomMapper.selectById(classId);
            if (room != null) {
                room.setStudentCount(count);
                classRoomMapper.updateById(room);
            }
            log.info("批量添加学生: classId={}, added={}", classId, toInsert.size());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeStudent(Long classId, Long studentId) {
        int affected = classStudentMapper.delete(new LambdaQueryWrapper<ClassStudent>()
                .eq(ClassStudent::getClassId, classId)
                .eq(ClassStudent::getStudentId, studentId));
        if (affected == 0) {
            throw new BizException(ErrorCode.NOT_IN_CLASS);
        }

        int count = classStudentMapper.countByClassId(classId);
        ClassRoom room = classRoomMapper.selectById(classId);
        if (room != null) {
            room.setStudentCount(count);
            classRoomMapper.updateById(room);
        }
        log.info("移除学生: classId={}, studentId={}", classId, studentId);
    }
}
