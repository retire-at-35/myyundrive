package com.pan.utils;



import com.pan.pojo.enums.VerifyRegexEnum;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VerifyUtils {

    public static boolean verify(String regex, String value) {
        if (StringTools.isEmpty(value)) {
            return false;
        }
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(value);
        return matcher.matches();
    }

    public static boolean verify(VerifyRegexEnum regex, String value) {
        return verify(regex.getRegex(), value);
    }

    public static boolean checkAccount(String account) {
        if (StringTools.isEmpty(account) || account.length() < 8 || account.length() > 20) {
            return false;
        }
        return verify(VerifyRegexEnum.ACCOUNT, account);
    }

    public static boolean checkPassWord(String password) {
        return verify(VerifyRegexEnum.PASSWORD, password);
    }
}

