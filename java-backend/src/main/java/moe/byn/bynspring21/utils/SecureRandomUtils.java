package moe.byn.bynspring21.utils;


import java.security.SecureRandom;

public final class SecureRandomUtils {
    private static final SecureRandom secureRandom = new SecureRandom();

    // 字符集：大小写字母 + 数字
    private static final String ALPHANUMERIC = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    /**
     * 闭区间
     * @param min
     * @param max
     * @return
     */
    public static int randomInt(int min, int max) {
        return min + secureRandom.nextInt(max - min + 1);
    }

    /**
     * 生成指定长度的随机字符串（包含大小写字母和数字）
     * @param length 字符串长度
     * @return 随机字符串
     */
    public static String randomString(int length) {
        if (length <= 0) {
            throw new IllegalArgumentException("长度必须大于0");
        }

        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int index = secureRandom.nextInt(ALPHANUMERIC.length());
            sb.append(ALPHANUMERIC.charAt(index));
        }
        return sb.toString();
    }
}
