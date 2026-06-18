package cn.smu.edu.common.result;

import com.baomidou.mybatisplus.core.metadata.IPage;

import java.util.List;

public record PageResult<T>(List<T> list, long total, long page, long size, long pages) {

    public static <T> PageResult<T> of(IPage<T> iPage) {
        return new PageResult<>(iPage.getRecords(), iPage.getTotal(),
                iPage.getCurrent(), iPage.getSize(), iPage.getPages());
    }

    public static <T> PageResult<T> of(List<T> list, long total, long page, long size) {
        long pages = size > 0 ? (total + size - 1) / size : 0;
        return new PageResult<>(list, total, page, size, pages);
    }
}
