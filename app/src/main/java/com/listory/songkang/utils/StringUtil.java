package com.listory.songkang.utils;

/**
 * Created by SouKou on 2017/8/28.
 */

public class StringUtil {

    public static boolean isEmpty(String s){
        if(s != null && !s.isEmpty()) {
            return false;
        }
        return true;
    }
}
