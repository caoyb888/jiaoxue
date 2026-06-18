package cn.smu.edu.course.repository;

import cn.smu.edu.course.domain.entity.CourseMaterial;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface CourseMaterialMapper extends BaseMapper<CourseMaterial> {

    IPage<CourseMaterial> selectMaterialPage(Page<CourseMaterial> page,
                                              @Param("teacherId") Long teacherId,
                                              @Param("keyword") String keyword);

    int updateConvertResult(@Param("materialId") Long materialId,
                            @Param("slideDir") String slideDir,
                            @Param("pageCount") int pageCount,
                            @Param("status") int status);
}
