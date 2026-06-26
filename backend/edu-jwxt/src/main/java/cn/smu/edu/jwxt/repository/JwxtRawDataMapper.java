package cn.smu.edu.jwxt.repository;

import cn.smu.edu.jwxt.domain.entity.JwxtRawData;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface JwxtRawDataMapper extends BaseMapper<JwxtRawData> {

    /** 批量插入原始数据（append-only，禁止循环单条 INSERT）。 */
    int batchInsert(@Param("list") List<JwxtRawData> list);

    /** 按数据类型拉取待处理（status=0）原始数据，限量分批，按 id 升序。 */
    List<JwxtRawData> selectPendingByType(@Param("dataType") String dataType,
                                          @Param("limit") int limit);

    /** 批量标记处理状态（成功/失败）。 */
    int updateStatusByIds(@Param("ids") List<Long> ids,
                          @Param("status") int status,
                          @Param("errorMsg") String errorMsg);
}
