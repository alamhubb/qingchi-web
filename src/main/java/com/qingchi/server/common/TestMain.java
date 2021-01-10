package com.qingchi.server.common;

import com.huaban.analysis.jieba.JiebaSegmenter;
import com.huaban.analysis.jieba.SegToken;
import com.qingchi.base.utils.KeywordsUtils;
import lombok.experimental.var;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TestMain {
    public static void main(String[] args) {
        String hasNot5Reg = ".*[^5]+.*";

        Pattern pattern = Pattern.compile(hasNot5Reg);
        //是否包含非5
        String text1 = "15";
        Matcher matcher = pattern.matcher(text1);

        //是否包含非5
        System.out.println(!matcher.matches());
        //全是5， 存在拒5，别5，这样的可能
        /*String str = "4654asfsdrq555erwesdaf456";
        String numAryReg = "\\d+";
        List<String> numAry = getMatchers(numAryReg, str);
        System.out.println(numAry.size());*/
    }

    static List<String> getMatchers(String regex, String source) {
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(source);
        List<String> list = new ArrayList<>();
        while (matcher.find()) {
            list.add(matcher.group());
        }
        return list;
    }

}
