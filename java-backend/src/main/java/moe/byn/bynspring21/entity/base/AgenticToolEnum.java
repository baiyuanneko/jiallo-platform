package moe.byn.bynspring21.entity.base;

import cn.hutool.core.text.CharSequenceUtil;
import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AgenticToolEnum {

    BOCHA_WEB_SEARCH("bocha_web_search", "博查网页搜索"),
    PYODIDE_CODE_RUNNER("pyodide_code_runner", "Pyodide代码执行"),
    MEMORY_TOOLS("memory_tools", "记忆工具"),
    WEB_FETCH("web_fetch", "网页内容抓取"),
    GET_DATETIME("get_datetime", "日期时间获取"),
    RAG_KNOWLEDGE_BASE("rag_knowledge_base", "RAG 知识库");

    @EnumValue
    @JsonValue
    private final String code;
    private final String desc;

    @JsonCreator
    public static AgenticToolEnum fromCode(String code) {
        for (AgenticToolEnum tool : values()) {
            if (tool.code.equals(code)) {
                return tool;
            }
        }
        return null;
    }

    public static String toToolCode(String methodName) {
        if (methodName == null || methodName.isEmpty()) return methodName;
        String snakeCase = CharSequenceUtil.toUnderlineCase(methodName);
        AgenticToolEnum tool = fromCode(snakeCase);
        return tool != null ? tool.getCode() : methodName;
    }
}
