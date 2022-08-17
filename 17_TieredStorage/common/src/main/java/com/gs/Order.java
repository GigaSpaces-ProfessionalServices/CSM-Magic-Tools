package com.gs;

import com.gigaspaces.annotation.pojo.SpaceId;
import com.gigaspaces.annotation.pojo.SpaceRouting;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;

@Entity
@Table(name = "Order")
public class Order implements Serializable {
    private Integer id;
    private String category;

    public Order(){     }


    public Order(Integer id, String category) {
        this.id = id;
        this.category = category;
    }
    @Id
    @SpaceRouting
    @SpaceId
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    @Override
    public String toString() {
        return "Order{" +
                "id=" + id +
                ", category='" + category + '\'' +
                '}';
    }
}
