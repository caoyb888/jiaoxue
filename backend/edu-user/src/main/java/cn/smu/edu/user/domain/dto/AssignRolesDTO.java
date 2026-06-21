package cn.smu.edu.user.domain.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 整体设置用户角色（全量替换）。roleIds 对应 RoleEnum 的 id。
 * 空列表表示清空该用户全部角色。
 */
@Data
public class AssignRolesDTO {

    @NotNull(message = "roleIds 不能为空")
    private List<Long> roleIds;
}
