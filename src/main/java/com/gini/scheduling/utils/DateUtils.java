package com.gini.scheduling.utils;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class DateUtils {
    

	/**
	 * 獲得指定時間格式的Date時間
	 * @author wangzhe@piesat.cn
	 * @date 2018年7月21日
	 * @param time 時間字符串
	 * @param plan 時間格式
	 * @return 時間
	 */
	public static Date getDateByString(String time,String plan){
    
		Long times = getLongTimeByString(time, plan);
		return  new Date(times);
	}

	/**
	 * 獲得指定時間格式的時間
	 * @author wangzhe@piesat.cn
	 * @date 2018年7月21日
	 * @param time 時間字符串
	 * @param plan 時間格式
	 * @return 時間
	 */
	public static String getStringDateByString(Date time,String plan){
    
		DateFormat format = new SimpleDateFormat(plan);
		return format.format(time);
	}

	/**
	 * 獲得指定時間格式的毫秒值
	 * @author wangzhe@piesat.cn
	 * @date 2018年7月17日
	 * @param time 時間字符串
	 * @param plan 時間格式
	 * @return 毫秒值
	 */
	public static Long getLongTimeByString(String time,String plan){
    
		DateFormat format = new SimpleDateFormat(plan);
		Long result = null;
		try {
    
			result = format.parse(time).getTime();
		} catch (ParseException e) {
    
			e.printStackTrace();
		}
		return  result;
	}
	
}