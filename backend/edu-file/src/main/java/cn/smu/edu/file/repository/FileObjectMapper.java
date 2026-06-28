package cn.smu.edu.file.repository;

import cn.smu.edu.file.domain.entity.FileObject;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface FileObjectMapper extends BaseMapper<FileObject> {

    /** 转冷存储：HOT → COLD，归档。 */
    @Update("UPDATE file_object SET storage_class = 'COLD', lifecycle_stage = 'ARCHIVED', "
            + "updated_at = NOW() WHERE id = #{id}")
    int markCold(@Param("id") Long id);

    /** 标记已删除（物理对象已从 MinIO 移除）。 */
    @Update("UPDATE file_object SET lifecycle_stage = 'DELETED', is_deleted = 1, "
            + "updated_at = NOW() WHERE id = #{id}")
    int markDeleted(@Param("id") Long id);
}
