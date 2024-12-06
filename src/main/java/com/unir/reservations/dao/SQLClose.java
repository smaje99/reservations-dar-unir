package com.unir.reservations.dao;

import java.sql.SQLException;

public interface SQLClose extends AutoCloseable {
  @Override
  void close() throws SQLException;
}
