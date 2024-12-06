package com.unir.reservations.utils;

import java.util.List;

public class Pagination {
  private Integer filterCounter;
  private List<?> data;

  public Pagination() {
  }

  public Pagination(Integer filterCounter, List<?> data) {
    this.filterCounter = filterCounter;
    this.data = data;
  }

  public Integer getFilterCounter() {
    return filterCounter;
  }

  public void setFilterCounter(Integer filterCounter) {
    this.filterCounter = filterCounter;
  }

  public List<?> getData() {
    return data;
  }

  public void setData(List<?> data) {
    this.data = data;
  }
}
