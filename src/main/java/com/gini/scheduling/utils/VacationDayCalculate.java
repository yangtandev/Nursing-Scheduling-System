package com.gini.scheduling.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gini.scheduling.controller.SgrroomController;
import com.gini.scheduling.utils.*;

/**
 * 從12月1號起計算一年中的假期
 */
public class VacationDayCalculate {
	public static final Logger logger = LoggerFactory.getLogger(SgrroomController.class);
	// 3天節假日規則=節日在周幾，放假日在周幾_放假之前的多少天上班_放假後的多少天上班;
	private String vacationWeek = "1,6-7-1_0_0;2,7-1-2_1_0;3,1-2-3_2_0;4,4-5-6_0_1;5,5-6-7_0_0;6,6-7-1_0_0;7,6-7-1_0_0";

	// 7天節假日規則=節日在周幾，放假之前的多少天上班_放假後的多少天上班;
	private String vacationMax = "1,2_0;2,2_0;3,3_0;4,0_3;5,0_2;6,0_2;7,1_1";

	/**
	 * 一個時間集合，放假則爲true，工作日爲false 放假包括國家法定節假日和雙休日
	 */
	public HashMap<String, Boolean> yearVacationDay(Integer year) {
		HashMap<String, Boolean> dates = weekVacation(year - 1);

		// 3天假日在周幾放假規則
		HashMap<Integer, String> weeks = new HashMap<>();
		String[] weeksTemp = vacationWeek.split(";");
		for (String weekStr : weeksTemp) {
			String[] week = weekStr.split(",");
			weeks.put(Integer.parseInt(week[0]), week[1]);
		}
		
		// 元旦節公曆一月一日，放假3天
		String vacationDay = year + "-01-01";
		setVacationThreeDay(vacationDay, dates, weeks);

		// 清明節(不分日曆，有規律)年份能整除4的年和下一年爲4月4日，再下兩年爲4月5日，放假3天
		int temp = year & 3;
		if (temp < 2) {
			vacationDay = year + "-04-04";
		} else {
			vacationDay = year + "-04-05";
		}
		setVacationThreeDay(vacationDay, dates, weeks);

		// 勞動節公曆五月一日，放假一天，沒有倒休
		dates.put("05-01", true);

		// 端午節農曆五月初五，放假3天
		vacationDay = lunar(year.toString(), 5, 5);
		setVacationThreeDay(vacationDay, dates, weeks);

		// 中秋節農曆八月十五，放假3天
		vacationDay = lunar(year.toString(), 8, 15);
		setVacationThreeDay(vacationDay, dates, weeks);

		// 國慶節公曆十月十號，放假3天
		vacationDay = year + "-10-10";
		setVacationThreeDay(vacationDay, dates, weeks);
				
		// 7天假日在周幾放假規則
		weeks.clear();
		weeksTemp = vacationMax.split(";");
		for (String weekStr : weeksTemp) {
			String[] week = weekStr.split(",");
			weeks.put(Integer.parseInt(week[0]), week[1]);
		}

		// 春節農曆一月初一，放假從前一天除夕開始，放假7天
		vacationDay = lunar(year.toString(), 1, 1);
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
		try {
			Long time = format.parse(vacationDay).getTime() - 86399000;
			vacationDay = format.format(time);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		setVacationSevenDay(vacationDay, dates, weeks);
		return dates;
	}

	/**
	 * 從12月1日起放入之後一年的時間 注：可通過代碼的註釋和釋放選擇是否加入雙休日
	 * @return map集合，key爲日期(不包括年)，value是true爲休息日，false爲工作日
	 */
	private HashMap<String, Boolean> weekVacation(Integer year) {
		// 放入一年的時間
		HashMap<String, Boolean> dates = new LinkedHashMap<>();
		Calendar cal = Calendar.getInstance();
		cal.setTime(DateUtils.getDateByString(year + "-12-1 12:00:00", "yyyy-MM-dd hh:mm:ss"));

		Integer days = 365;
		if ((year & 3) == 0) {
			days = 366;
		}
		for (int i = 0, j = 0; i < days; i++, j = 1) {
			cal.add(Calendar.DAY_OF_YEAR, j);
			String date = DateUtils.getStringDateByString(cal.getTime(), "MM-dd");
			// 這裏加入雙休
			Integer ifVacation = dayForWeek(cal.getTime().getTime());
			if (ifVacation == 6 || ifVacation == 7) {
				dates.put(date, true);
			} else {
				dates.put(date, false);
			}
			// 若不需要雙休，只需要法定假日，則將上面的幾行註釋掉，放開下面這一行
//			dates.put(date, false);
		}
		return dates;
	}

	/**
	 * 3天假期 計算放假日期和上班日期並修改
	 * @param vacationDay 節日
	 * @param dates 時間集合
	 * @param weeks 放假周規律集合
	 */
	private void setVacationThreeDay(String vacationDay, HashMap<String, Boolean> dates,
			HashMap<Integer, String> weeks) {
		Integer week = dayForWeek(vacationDay); 
		String[] vacation = weeks.get(week).split("_"); 
		int indexOf = vacation[0].indexOf(week.toString());
		Integer[] interval = dayForWeekThree(indexOf);
		Integer incr = Integer.parseInt(vacation[1]);
		Integer decr = Integer.parseInt(vacation[2]);
		List<String> vacationDate = dayForWeek(vacationDay, interval[0], interval[1]);
		for (String day : vacationDate) {
			dates.put(day, true);
		}
		List<String> workDate = dayForWork(vacationDay, interval[0], interval[1], incr, decr);
		for (String day : workDate) {
			dates.put(day, false);
		}
	}

	/**
	 * 7天假期 計算放假日期和上班日期並修改
	 * @param vacationDay 節日
	 * @param dates 時間集合
	 * @param weeks 放假周規律集合
	 */
	private void setVacationSevenDay(String vacationDay, HashMap<String, Boolean> dates,
			HashMap<Integer, String> weeks) {
		Integer week = dayForWeek(vacationDay);
		String[] vacation = weeks.get(week).split("_");
		Integer incr = Integer.parseInt(vacation[0]);
		Integer decr = Integer.parseInt(vacation[1]);
		List<String> vacationDate = dayForWeek(vacationDay, 0, 6);
		for (String day : vacationDate) {
			dates.put(day, true);
		}
		List<String> workDate = dayForWork(vacationDay, 0, 6, incr, decr);
		for (String day : workDate) {
			dates.put(day, false);
		}
	}

	/**
	 * 獲得指定字符串的時間是星期幾
	 * @param pTime 時間字符串
	 */
	private Integer dayForWeek(String pTime) {
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
		Calendar cal = Calendar.getInstance();
		try {
			Date tmpDate = format.parse(pTime);
			cal.setTime(tmpDate);
		} catch (Exception e) {
			e.printStackTrace();
		}
		int w = cal.get(Calendar.DAY_OF_WEEK) - 1; // 指示一個星期中的某天。
		if (w < 0)
			w = 0;
		if (w == 0)
			w = 7;
		return w;
	}

	/**
	 * 獲得指定毫秒數的時間是星期幾
	 * @param pTime 時間字符串
	 */
	private Integer dayForWeek(Long pTime) {
		Calendar cal = Calendar.getInstance();
		try {
			cal.setTime(new Date(pTime));
		} catch (Exception e) {
			e.printStackTrace();
		}
		int w = cal.get(Calendar.DAY_OF_WEEK) - 1; // 指示一個星期中的某天。
		if (w < 0)
			w = 0;
		if (w == 0)
			w = 7;
		return w;
	}

	/**
	 * 放假三天的假日放假時間
	 * @param indexOf 節日在周幾
	 * @return 放假時間範圍在節日之前開始和幾日之後結束
	 */
	private Integer[] dayForWeekThree(Integer indexOf) {
		Integer incr, decr;
		if (indexOf == 0) {
			incr = 0;
			decr = 2;
		} else if (indexOf == 2) {
			incr = 1;
			decr = 1;
		} else {
			incr = 2;
			decr = 0;
		}
		return new Integer[] { incr, decr };
	}

	/**
	 * 獲得指定時間前幾天的日期或者後幾天的日期
	 * @param pTime 時間
	 * @param incr 之前幾天
	 * @param decr 之後幾天
	 * @return 放假時間日期集合
	 */
	private List<String> dayForWeek(String pTime, Integer incr, Integer decr) {
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
		SimpleDateFormat rformat = new SimpleDateFormat("MM-dd");
		List<String> result = new ArrayList<>(10);
		Calendar cal = Calendar.getInstance();
		try {
			Date tmpDate = format.parse(pTime);
			result.add(rformat.format(tmpDate));
			cal.setTime(tmpDate);
		} catch (Exception e) {
			e.printStackTrace();
		}

		for (int i = 0; i < incr; i++) {
			cal.add(Calendar.DAY_OF_YEAR, -1);
			result.add(rformat.format(cal.getTime()));
		}

		cal.add(Calendar.DAY_OF_YEAR, 1 * incr);
		for (int i = 0; i < decr; i++) {
			cal.add(Calendar.DAY_OF_YEAR, 1);
			result.add(rformat.format(cal.getTime()));
		}
		return result;
	}

	/**
	 * 獲得放假之前和之後需要上班的時間
	 * @param pTime 節日時間
	 * @param v_incr 節日之前幾天開始放假
	 * @param v_decr 節日之後幾天開始放假
	 * @param w_incr 節日之前工作幾天
	 * @param w_decr 節日之後工作幾天
	 * @return 節假日前後需要上班的週六日時間
	 */
	private List<String> dayForWork(String pTime, Integer v_incr, Integer v_decr, Integer w_incr, Integer w_decr) {
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
		SimpleDateFormat rformat = new SimpleDateFormat("MM-dd");
		Calendar cal = Calendar.getInstance();
		try {
			Date tmpDate = format.parse(pTime);
			cal.setTime(tmpDate);
		} catch (Exception e) {
			e.printStackTrace();
		}
		List<String> result = new ArrayList<>(5);

		cal.add(Calendar.DAY_OF_YEAR, v_incr * -1);
		for (int i = 0; i < w_incr; i++) {
			cal.add(Calendar.DAY_OF_YEAR, -1);
			result.add(rformat.format(cal.getTime()));
		}

		cal.add(Calendar.DAY_OF_YEAR, v_incr + w_incr + v_decr);
		for (int i = 0; i < w_decr; i++) {
			cal.add(Calendar.DAY_OF_YEAR, 1);
			result.add(rformat.format(cal.getTime()));
		}
		return result;
	}

	/**
	 * 根據傳入的農曆日期計算公曆日期
	 * @param year 年
	 * @param month 月
	 * @param day 日
	 * @return "年-月-日"
	 */
	public String lunar(String year, Integer month, Integer day) {
		Calendar cal = Calendar.getInstance();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		try {
			cal.setTime(sdf.parse(year + "-" + month + "-" + day));
		} catch (ParseException e) {
			e.printStackTrace();
		}
		calcuLunar(cal, month, day, new Lunar(true));
		return year + "-" + (cal.get(Calendar.MONTH) + 1) + "-" + cal.get(Calendar.DAY_OF_MONTH);
	}

	/**
	 * 農曆轉公曆計算
	 * @param cal 日期對象
	 * @param month 月
	 * @param day 日
	 */
	public static void calcuLunar(Calendar cal, int month, int day, Lunar l) {
		Lunar lunar = new Lunar(cal);
		if (lunar.getMonth() != month) {
			cal.add(Calendar.MONTH, 1);
			calcuLunar(cal, month, day, l);
		}
		if (lunar.getDay() != day && l.isLeap()) {
			if (lunar.getDay() > day) {
				cal.add(Calendar.DAY_OF_YEAR, -1);
			} else {
				cal.add(Calendar.DAY_OF_YEAR, 1);
			}
			calcuLunar(cal, month, day, l);
			if (l.isLeap()) {
				l.setLeap(false);
			}
		}
	}

	/**
	 * 進行測試，指定月份打印，要不然數字太多，正確性不好比對
	 */
//	public static void main(String[] args) {
//		HashMap<String, Boolean> map = new VacationDayCalculate().yearVacationDay(2022);
//		Set<String> keySet = map.keySet();
//		for (String key : keySet) {
//			Boolean v = map.get(key);
//			if (v && key.startsWith("10")) {
//				System.out.println(key);
//			}
//
//		}
//	}
}