package com.ynp.agent.model.param;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Schema(description = "会话创建/修改参数")
@Data
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class ConversationParam {

    @Schema(description = "会话id")
    private Long id;
    @Schema(description = "会话类型")
    @NotNull(message = "会话类型不能为空")
    private Integer type;

    @Schema(description = "会话名")
    private String title;

    @Schema(description = "涉及的成员id集合")
    @NotNull(message = "成员id不能为空")
    private List<Long> memberIds;
}
