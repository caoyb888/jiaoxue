package cn.smu.edu.jwxt.repository;

import cn.smu.edu.jwxt.domain.entity.JwxtIdMapping;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface JwxtIdMappingMapper extends BaseMapper<JwxtIdMapping> {

    /**
     * 按数据类型和教务ID查本系统ID（核心查找路径）
     */
    Long selectLocalIdByJwxtId(@Param("dataType") String dataType,
                                @Param("jwxtId") String jwxtId);

    /**
     * 按数据类型和本系统ID查教务ID（反查路径）
     */
    String selectJwxtIdByLocalId(@Param("dataType") String dataType,
                                  @Param("localId") Long localId);

    /**
     * 批量 upsert（INSERT ON DUPLICATE KEY UPDATE）
     * 用于增量同步时高效写入或更新映射关系
     */
    int batchUpsert(@Param("list") List<JwxtIdMapping> mappings);
}
