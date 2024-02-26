package com.fastpan.utils;

import org.springframework.beans.BeanUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 就是BeanUtils.copyProperties(s, t)
 * 搞这么多Utils干嘛
 */
public class CopyTools {
    //拷贝list
    public static <T, S> List<T> copyList(List<S> sList, Class<T> classz) {
        List<T> list = new ArrayList<T>();
        for (S s : sList) {
            T t = null;
            try {
                //生成T类的实例化对象
                t = classz.newInstance();
            } catch (Exception e) {
                e.printStackTrace();
            }
            // 向t中注入s属性
            BeanUtils.copyProperties(s, t);
            list.add(t);
        }
        return list;
    }

    //拷贝对象
    public static <T, S> T copy(S s, Class<T> classz) {
        T t = null;
        try {
            t = classz.newInstance();
        } catch (Exception e) {
            e.printStackTrace();
        }
        BeanUtils.copyProperties(s, t);
        return t;
    }
}
