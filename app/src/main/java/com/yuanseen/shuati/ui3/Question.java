package com.yuanseen.shuati.ui3;

public class Question {
    public enum Type { SINGLE_CHOICE, MULTI_CHOICE, TRUE_FALSE }

    private final String bankId;      // 所属题库ID
    private final Type type;          // 题目类型
    private final int number;         // 题号
    private final String content;     // 题目内容
    private final String options;     // 选项
    private final String answer;      // 答案

    public Question(String bankId, Type type, int number, String content, String options, String answer) {
        this.bankId = bankId;
        this.type = type;
        this.number = number;
        this.content = content;
        this.options = options;
        this.answer = answer;
    }

    // Getter 方法
    public String getBankId() { return bankId; }
    public Type getType() { return type; }
    public int getNumber() { return number; }
    public String getContent() { return content; }
    public String getOptions() { return options; }
    public String getAnswer() { return answer; }
}
