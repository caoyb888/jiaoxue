package cn.smu.edu.jwxt.adapter;

import java.util.List;

/**
 * 教务数据类型常量（jwxt_raw_data.data_type 取值）。
 */
public final class JwxtDataType {

    public static final String STUDENT = "STUDENT";
    public static final String DEPT = "DEPT";
    public static final String COURSE = "COURSE";
    public static final String CLASS = "CLASS";

    /** 增量同步遍历的数据类型顺序（院系/课程先于学生/教学班，便于外键对照）。 */
    public static final List<String> SYNC_ORDER = List.of(DEPT, COURSE, STUDENT, CLASS);

    private JwxtDataType() {
    }
}
