package utils;

import java.util.regex.Pattern;

/**
 * Created by wanny-n1 on 2017/6/6.
 */

public class RegexUtils {
    /**
     * 验证是否为网址
     *
     * @param url
     * @return
     */
    public static boolean isUrl(String url) {
        Pattern pattern = Pattern.compile("(http|ftp|https):\\/\\/[\\w\\-_]+(\\.[\\w\\-_]+)+([\\w\\-\\.,@?^=%&amp;:/~\\+#]*[\\w\\-\\@?^=%&amp;/~\\+#])?");
        return pattern.matcher(url).matches();
    }
}
