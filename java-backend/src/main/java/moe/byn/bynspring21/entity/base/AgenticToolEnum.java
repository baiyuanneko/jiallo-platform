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
    RAG_KNOWLEDGE_BASE("rag_knowledge_base", "RAG 知识库"),
    EXECUTE_SHELL_COMMAND("execute_shell_command", "Shell命令执行"),
    READ_FILE("read_file", "文件读取"),
    WRITE_FILE("write_file", "文件写入"),
    EDIT_FILE("edit_file", "文件编辑"),
    LIST_FILES("list_files", "目录列举"),
    SEARCH_FILES("search_files", "文件搜索"),
    DEEPSEEK_WEB_SEARCH("deepseek_web_search", "DeepSeek网页搜索"),
    DEEPSEEK_WEB_FETCH("deepseek_web_fetch", "DeepSeek网页抓取");

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
