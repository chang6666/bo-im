package com.james.platform.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * @author james
 */
@Data
public class FriendVO {

    @NotNull(message = "好友id不可为空")
    @Schema(description=  "好友id")
    private Long id;

    @NotNull(message = "好友昵称不可为空")
    @Schema(description=  "好友昵称")
    private String nickName;


    @Schema(description=  "好友头像")
    private String headImage;

}
