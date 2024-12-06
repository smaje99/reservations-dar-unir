package com.unir.reservations.utils;

public class RawRecord {
  private String serverMessage;
  private Pagination pagination;

  public RawRecord() {
  }

  public RawRecord(Pagination pagination) {
    this.pagination = pagination;
  }

  public String getServerMessage() {
    return serverMessage;
  }

  public void setServerMessage(String serverMessage) {
    this.serverMessage = serverMessage;
  }

  public Pagination getPagination() {
    return pagination;
  }

  public void setPagination(Pagination pagination) {
    this.pagination = pagination;
  }
}
