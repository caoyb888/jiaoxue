package cn.smu.edu.course.repository;

import cn.smu.edu.course.domain.entity.ClassRoom;
import cn.smu.edu.course.domain.vo.ClassRoomVO;
import cn.smu.edu.course.domain.vo.ClassRoomDetailVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ClassRoomMapper extends BaseMapper<ClassRoom> {

    List<ClassRoomVO> selectMyClasses(
            @Param("userId") Long userId,
            @Param("userType") Integer userType,
            @Param("semester") String semester,
            @Param("status") Integer status);

    ClassRoomDetailVO selectClassDetail(@Param("classId") Long classId);
}
