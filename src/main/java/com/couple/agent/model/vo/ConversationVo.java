package com.couple.agent.model.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationVo {


    private Long id;


    private String title;


    private LocalDateTime createTime;


    private LocalDateTime lastMessageAt;
}
