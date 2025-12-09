package com.couple.agent.model.param;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Schema(description = "会话创建/修改参数")
@Data
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class ConversationParam {

    @Schema(description = "会话id")
    private Long id;

    @Schema(description = "会话名")
    private String title;
}
