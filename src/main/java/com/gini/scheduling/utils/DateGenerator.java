package com.gini.scheduling.utils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class DateGenerator {
    /**
     * 獲取當前時間所在月有多少週
     *
     * @param year, month
     * @return weeks
     */
    public int weeksOfMonth(String year, String month) {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.YEAR, Integer.parseInt(year));
        c.set(Calendar.MONTH, Integer.parseInt(month));
        c.setFirstDayOfWeek(Calendar.MONDAY);
        return c.WEEK_OF_MONTH;
    }

    //算出所在周的周日
    public String getSunOfWeek(String time) {
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        // 起始日期
        LocalDate localDateate = LocalDate.parse(time, dateTimeFormatter);
        LocalDate endday = localDateate.with(TemporalAdjusters.next(java.time.DayOfWeek.MONDAY)).minusDays(1);
        return endday.format(dateTimeFormatter);
    }

    //下一周的周一
    public String getLastMonOfWeek(String time) {
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        // 起始日期
        LocalDate localDateate = LocalDate.parse(time, dateTimeFormatter);
        LocalDate endday = localDateate.with(TemporalAdjusters.next(java.time.DayOfWeek.MONDAY));
        return endday.format(dateTimeFormatter);
    }

    /**
     * 獲取本月週區間(每週起始和結束時間為周一到週日)
     *
     * @param year, month
     * @return weekInfos
     */
    public List<WeekInfo> getScope(String year, String month) {
        month = Integer.parseInt(month) < 10 ? "0" + month : month;
        int weeks = weeksOfMonth(year, month);
        List<WeekInfo> weekInfos = new ArrayList<>();
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        // 起始日期
        LocalDate localDateate = LocalDate.parse(String.format("%s-%s-01", year, month), dateTimeFormatter);

        //月份第一周的起始時間和結束時間
        LocalDate firstDay = localDateate.with(TemporalAdjusters.firstDayOfMonth());
        String firstDayStr = firstDay.format(dateTimeFormatter);
        String sunStr = getSunOfWeek(firstDayStr);

        for (int i = 1; i <= weeks; i++) {
            WeekInfo weekInfo = new WeekInfo();
            //第一周的起始時間就是當月的1號，結束時間就是周日
            if (i == 1) {
                weekInfo.setStart(firstDayStr);
                weekInfo.setEnd(sunStr);
                weekInfo.setOrder(i);
                //計算接下來每週的周一和周日
            } else if (i < weeks) {
                //由於sunStr是上一周的周日，所以取周一要取sunStr的下一周的周一
                String monDay = getLastMonOfWeek(sunStr);
                sunStr = getSunOfWeek(monDay);
                weekInfo.setOrder(i);
                weekInfo.setStart(monDay);
                weekInfo.setEnd(sunStr);
                //由於最後一周可能結束時間不是周日，所以要單獨處理
            } else {

                String monDay = getLastMonOfWeek(sunStr);
                //結束時間肯定就是當前月的最後一天
                LocalDate lastDay = localDateate.with(TemporalAdjusters.lastDayOfMonth());
                String endDay = lastDay.format(dateTimeFormatter);
                weekInfo.setOrder(i);
                weekInfo.setStart(monDay);
                weekInfo.setEnd(endDay);
            }
            weekInfos.add(weekInfo);

        }
        return weekInfos;
    }

    public static class WeekInfo {
        private String start;
        private String end;
        private Integer order;

        public String getStart() {
            return start;
        }

        public void setStart(String start) {
            this.start = start;
        }

        public String getEnd() {
            return end;
        }

        public void setEnd(String end) {
            this.end = end;
        }

        public Integer getOrder() {
            return order;
        }

        public void setOrder(Integer order) {
            this.order = order;
        }
    }
}