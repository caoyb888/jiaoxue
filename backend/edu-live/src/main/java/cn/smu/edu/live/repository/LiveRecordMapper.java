package cn.smu.edu.live.repository;

import cn.smu.edu.live.domain.entity.LiveRecord;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface LiveRecordMapper extends BaseMapper<LiveRecord> {

    /** 按课堂查直播记录（uk_lesson_id 唯一）。 */
    @Select("SELECT id, lesson_id, stream_key, push_url, play_url, replay_path, duration_sec, "
            + "status, started_at, ended_at, created_at FROM live_record WHERE lesson_id = #{lessonId}")
    LiveRecord selectByLessonId(@Param("lessonId") Long lessonId);

    /** 按推流密钥查直播记录（SRS 回调以 streamKey 定位）。 */
    @Select("SELECT id, lesson_id, stream_key, push_url, play_url, replay_path, duration_sec, "
            + "status, started_at, ended_at, created_at FROM live_record WHERE stream_key = #{streamKey}")
    LiveRecord selectByStreamKey(@Param("streamKey") String streamKey);
}
