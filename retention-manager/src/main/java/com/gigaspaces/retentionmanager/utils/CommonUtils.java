package com.gigaspaces.retentionmanager.utils;

import com.j_spaces.jdbc.driver.GConnection;
import org.openspaces.core.GigaSpace;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

@Component
public class CommonUtils {



    /**
     *
     * @param start_date
     * @param end_date
     * @param unit -- Values (D=days,Y=year,H=hours,M=minute,S=seconds)
     * @return
     */
    public long findDateDifference(Date start_date, Date end_date, String unit) throws Exception{

        SimpleDateFormat sdf  = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");

        long dateDifference = 0;


        long difference_In_Time = end_date.getTime() - start_date.getTime();

        long difference_In_Seconds= (difference_In_Time  / 1000)  % 60;

        long difference_In_Minutes  = (difference_In_Time  / (1000 * 60)) % 60;

        long difference_In_Hours  = (difference_In_Time / (1000 * 60 * 60)) % 24;

        long difference_In_Years = (difference_In_Time / (1000l * 60 * 60 * 24 * 365));

        long difference_In_Days = (difference_In_Time / (1000 * 60 * 60 * 24)) % 365;

        switch (unit){
            case "D":
                dateDifference =  (difference_In_Time / (1000 * 60 * 60 * 24)) % 365;
                break;

            case "Y":
                dateDifference =  (difference_In_Time / (1000l * 60 * 60 * 24 * 365));
                break;

            case "H":
                dateDifference =  (difference_In_Time / (1000 * 60 * 60)) % 24;
                break;

            case "M":
                dateDifference =  (difference_In_Time  / (1000 * 60)) % 60;
                break;

            case "S":
                dateDifference =  (difference_In_Time  / 1000)  % 60;
                break;

            default:
                dateDifference = difference_In_Time;

        }


        return dateDifference;
    }

    /**
     * Pass negative value to calculate previous date
     * @param date
     * @param unit
     * @param duration
     * @return
     */
    public Date addSubstractFromDate(Date date, int duration, String unit){

        if(date!=null) {
            Calendar c = Calendar.getInstance();
            c.setTime(date);
            switch (unit) {
                case "d":
                    c.add(Calendar.DATE, duration);
                    break;

                case "y":
                    c.add(Calendar.YEAR, duration);
                    break;

                case "h":
                    c.add(Calendar.HOUR, duration);
                    break;

                case "M":
                    c.add(Calendar.MONTH, duration);
                    break;

                case "m":
                    c.add(Calendar.MINUTE, duration);
                    break;

                case "s":
                    c.add(Calendar.SECOND, duration);
                    break;

                default:

            }
            Date newDate = c.getTime();
            return newDate;
        }

        return null;
    }


}
