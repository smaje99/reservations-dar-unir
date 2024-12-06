package com.unir.reservations.dao;

import java.sql.Connection;
import java.util.HashMap;

import com.unir.reservations.utils.Pagination;
import com.unir.reservations.utils.RawRecord;

public interface CRUD<T> {
    Pagination getList(HashMap<String, Object> params, Connection connection);
    RawRecord getList(HashMap<String, Object> params);
    RawRecord add(T object, HashMap<String, Object> params);
    RawRecord update(T object, HashMap<String, Object> params);
    RawRecord delete(Integer id, HashMap<String, Object> params);
    T getForId(Integer id);
}
