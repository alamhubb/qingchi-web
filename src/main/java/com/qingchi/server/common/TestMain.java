package com.qingchi.server.common;

import com.qingchi.base.utils.KeywordsUtils;

import java.util.*;

public class TestMain {
    public static void main(String[] args) {
        String text = "找个18到22的小姐姐，来打王者呀，上单凯爹在此。技术一般，不嫌弃的留微信！";
        Map<String, Integer> keyMap = KeywordsUtils.chineseWordSegmentationGetKeywordsMap(text);

        Map<String, Integer> sortMap = sortMapByValue(keyMap);
        for (Map.Entry<String, Integer> entry : sortMap.entrySet()) {
            System.out.println(entry.getKey());
            System.out.println(entry.getValue());
        }
        System.out.println(keyMap.size());
    }

    public static Map<String, Integer> sortMapByValue(Map<String, Integer> oriMap) {
        if (oriMap == null || oriMap.isEmpty()) {
            return null;
        }
        Map<String, Integer> sortedMap = new LinkedHashMap<>();
        List<Map.Entry<String, Integer>> entryList = new ArrayList<>(
                oriMap.entrySet());
        entryList.sort(new MapValueComparator());

        Iterator<Map.Entry<String, Integer>> iter = entryList.iterator();
        Map.Entry<String, Integer> tmpEntry;
        while (iter.hasNext()) {
            tmpEntry = iter.next();
            sortedMap.put(tmpEntry.getKey(), tmpEntry.getValue());
        }
        return sortedMap;
    }

    static class MapValueComparator implements Comparator<Map.Entry<String, Integer>> {

        @Override
        public int compare(Map.Entry<String, Integer> me1, Map.Entry<String, Integer> me2) {

            return me1.getValue().compareTo(me2.getValue());
        }
    }
}
