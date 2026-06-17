package moe.byn.bynspring21.utils;

import com.github.houbb.sensitive.word.core.SensitiveWordHelper;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SensitiveWordUtils {
    public static Boolean contains(String text) {
        return SensitiveWordHelper.contains(text);
    }
}
