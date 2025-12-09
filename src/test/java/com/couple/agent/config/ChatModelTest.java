package com.couple.agent.config;

import com.couple.agent.assistant.LoveAssistant;
import dev.langchain4j.model.chat.ChatModel;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class ChatModelTest {

    @Autowired
    LoveAssistant assistant;

    @Autowired
    ChatModel azureOpenAiChatModel;

    @Test
    public void chatModelTest(){

        System.out.println(azureOpenAiChatModel.chat("你好，你是谁？"));

    }

    @Test
    public void aiAssistantTest(){
        assistant.chat(1,"你是谁？");
    }
}
