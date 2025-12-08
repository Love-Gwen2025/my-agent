package com.ynp.agent.model.dto;

import java.time.LocalDateTime;

public record UserProfileResponse(Long id,
                                  String userCode,
                                  String userName,
                                  String userPhone,
                                  LocalDateTime createTime,
                                  LocalDateTime updateTime) {
}
