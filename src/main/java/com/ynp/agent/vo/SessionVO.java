package com.ynp.agent.vo;

import lombok.Data;

import java.util.List;

/**
 * 会话状态 VO。
 */
@Data
public class SessionVO {
    private boolean authenticated;
    private UserVO user;
    private String assistantName;
    private List<AccountVO> accounts;
}
