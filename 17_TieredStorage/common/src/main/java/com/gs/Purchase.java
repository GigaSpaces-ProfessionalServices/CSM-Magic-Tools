package com.gs;

import com.gigaspaces.annotation.pojo.SpaceId;
import com.gigaspaces.annotation.pojo.SpaceRouting;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.Date;

@Entity
@Table(name = "Purchase")
public class Purchase {
    private int id;
    private Date orderTime;
    private String weekDay;

    public Purchase(){

    }

    public Purchase(int id, Date orderTime, String weekDay){
        this.id = id;
        this.orderTime = orderTime;
        this.weekDay = weekDay;
    }

    @Id
    @SpaceRouting
    @SpaceId
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Date getOrderTime() {
        return orderTime;
    }

    public void setOrderTime(Date orderTime) {
        this.orderTime = orderTime;
    }

    public String getWeekDay() {
        return weekDay;
    }

    public void setWeekDay(String weekDay) {
        this.weekDay = weekDay;
    }

    @Override
    public String toString() {
        return "Purchase{" +
                "id=" + id +
                ", orderTime=" + orderTime +
                ", weekDay="+weekDay+
                '}';
    }
}
