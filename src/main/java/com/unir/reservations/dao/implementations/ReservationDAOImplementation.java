package com.unir.reservations.dao.implementations;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.sql.DataSource;

import com.unir.reservations.dao.ClientDAO;
import com.unir.reservations.dao.ReservationDAO;
import com.unir.reservations.dao.RoomDAO;
import com.unir.reservations.dao.SQLClose;
import com.unir.reservations.models.Reservation;
import com.unir.reservations.utils.Pagination;
import com.unir.reservations.utils.RawRecord;

public class ReservationDAOImplementation implements ReservationDAO {
  private static final Logger LOGGER = Logger.getLogger(ReservationDAOImplementation.class.getName());
  private final DataSource pool;

  private static final String GET_LIST_COUNT = """
    SELECT COUNT(eventId) AS COUNT
    FROM evp_event
    WHERE clientId LIKE CONCAT('%', ?, '%')
  """;
  private static final String GET_LIST_QUERY = """
    SELECT
      eventId,
      clientId,
      roomId,
      date,
      startHour,
      endHour,
      priceTotal,
      observations
    FROM evp_event
    WHERE clientId LIKE CONCAT('%', ?, '%')
  """;
  private static final String ADD_COUNT = """
    SELECT COUNT(eventId) AS COUNT
    FROM evp_event
    WHERE eventId = ?
  """;
  private static final String ADD_QUERY = """
    INSERT INTO evp_event (
      clientId,
      roomId,
      date,
      startHour,
      endHour,
      priceTotal,
      observations
    ) VALUES (?, ?, ?, ?, ?, ?, ?)
  """;
  private static final String UPDATE_COUNT = """
    SELECT COUNT(eventId) AS COUNT
    FROM evp_event
    WHERE clientId = ? AND eventId != ?
  """;
  private static final String UPDATE_QUERY = """
    UPDATE evp_event
    SET
      clientId = ?,
      roomId = ?,
      date = ?,
      startHour = ?,
      endHour = ?,
      priceTotal = ?,
      observations = ?
    WHERE eventId = ?
  """;
  private static final String DELETE_QUERY = """
    DELETE FROM evp_event
    WHERE eventId = ?
  """;
  private static final String GET_FOR_ID = """
    SELECT
      eventId,
      clientId,
      roomId,
      date,
      startHour,
      endHour,
      priceTotal,
      observations
    FROM evp_event
    WHERE eventId = ?
  """;

  public ReservationDAOImplementation(DataSource pool) {
    this.pool = pool;
  }

  @Override
  public Pagination getList(HashMap<String, Object> params, Connection connection) {
    final Pagination pagination = new Pagination();
    final List<Reservation> reservations = new ArrayList<>();

    try(
      PreparedStatement statementCount = connection.prepareStatement(GET_LIST_COUNT)
    ) {
      statementCount.setString(1, (String) params.get("FILTER"));

      LOGGER.log(Level.INFO, "Executing query: {0}", statementCount);

      try(ResultSet resultSet = statementCount.executeQuery()) {
        if(resultSet.next()) {
          final int count = resultSet.getInt("COUNT");
          pagination.setFilterCounter(count);

          if (count > 0) {
            try (
              PreparedStatement statementQuery = connection.prepareStatement(
                GET_LIST_QUERY +
                (String) params.get("SQL_ORDER_BY") +
                (
                  params.containsKey("SQL_PAGINATION")
                    ? " " + (String) params.get("SQL_PAGINATION")
                    : ""
                )
              )
            ) {
              statementQuery.setString(1, (String) params.get("FILTER"));

              LOGGER.log(Level.INFO, "Executing query: {0}", statementQuery);

              try(ResultSet resultSetQuery = statementQuery.executeQuery()) {
                final ClientDAO clientDAO = new ClientDAOImplementation(pool);
                final RoomDAO roomDAO = new RoomDAOImplementation(pool);

                while(resultSetQuery.next()) {
                  final Reservation reservation = new Reservation(
                    resultSetQuery.getInt("eventId"),
                    clientDAO.getForId(resultSetQuery.getInt("clientId")),
                    null,
                    roomDAO.getForId(resultSetQuery.getInt("roomId")),
                    resultSetQuery.getDate("date"),
                    resultSetQuery.getInt("startHour"),
                    resultSetQuery.getInt("endHour"),
                    resultSetQuery.getDouble("priceTotal"),
                    resultSetQuery.getString("observations")
                  );

                  reservations.add(reservation);
                }
              }
            }
          }
        }
      }
    } catch(SQLException e) {
      LOGGER.log(Level.SEVERE, "Error updating client: {0}", e.getMessage());
    }

    pagination.setData(reservations);
    return pagination;
  }

  @Override
  public RawRecord getList(HashMap<String, Object> params) {
    final RawRecord rawRecord = new RawRecord();

    try(Connection connection = pool.getConnection()) {
      rawRecord.setPagination(getList(params, connection));
    } catch(SQLException e) {
      LOGGER.log(Level.SEVERE, "Error getting list: {0}", e.getMessage());
      rawRecord.setServerMessage("Error getting list");
    }

    return rawRecord;
  }

  @Override
  public RawRecord add(Reservation reservation, HashMap<String, Object> params) {
    final RawRecord rawRecord = new RawRecord();

    try(
      Connection connection = pool.getConnection();
      SQLClose finish = connection::close;
      PreparedStatement statementCount = connection.prepareStatement(ADD_COUNT)
    ) {
      connection.setAutoCommit(false);

      statementCount.setInt(1, reservation.idReservation());

      LOGGER.log(Level.INFO, "Executing query: {0}", statementCount);

      try(ResultSet resultSet = statementCount.executeQuery()) {
        if (resultSet.next()) {
          final int count = resultSet.getInt("COUNT");
          if (count > 0) {
            rawRecord.setServerMessage("Reservation already exists");
            return rawRecord;
          }

          try(
            PreparedStatement statementQuery = connection.prepareStatement(ADD_QUERY)
          ) {
            statementQuery.setInt(1, reservation.client().idClient());
            statementQuery.setInt(2, reservation.room().idRoom());
            statementQuery.setDate(3, new Date(reservation.date().getTime()));
            statementQuery.setInt(4, reservation.startHour());
            statementQuery.setInt(5, reservation.endHour());
            statementQuery.setDouble(6, reservation.priceTotal());
            statementQuery.setString(7, reservation.observations());

            LOGGER.log(Level.INFO, "Executing query: {0}", statementQuery);

            statementQuery.executeUpdate();
            connection.commit();

            rawRecord.setServerMessage("Reservation added successfully");
            rawRecord.setPagination(getList(params, connection));
          }
        }
      }
    } catch(SQLException e) {
      LOGGER.log(Level.SEVERE, "Error updating client: {0}", e.getMessage());
      rawRecord.setServerMessage("Error adding client");
    }

    return rawRecord;
  }

  @Override
  public RawRecord update(Reservation object, HashMap<String, Object> params) {
    final RawRecord rawRecord = new RawRecord();

    try(
      Connection connection = pool.getConnection();
      SQLClose finish = connection::close;
      PreparedStatement statementCount = connection.prepareStatement(UPDATE_COUNT)
    ) {
      connection.setAutoCommit(false);

      statementCount.setInt(1, object.client().idClient());
      statementCount.setInt(2, object.idReservation());

      LOGGER.log(Level.INFO, "Executing query: {0}", statementCount);

      try(ResultSet resultSet = statementCount.executeQuery()) {
        if (resultSet.next()) {
          final int count = resultSet.getInt("COUNT");
          if (count > 0) {
            rawRecord.setServerMessage("Reservation already exists");
            return rawRecord;
          }

          try(
            PreparedStatement statementQuery = connection.prepareStatement(UPDATE_QUERY)
          ) {
            statementQuery.setInt(1, object.client().idClient());
            statementQuery.setInt(2, object.room().idRoom());
            statementQuery.setDate(3, new Date(object.date().getTime()));
            statementQuery.setInt(4, object.startHour());
            statementQuery.setInt(5, object.endHour());
            statementQuery.setDouble(6, object.priceTotal());
            statementQuery.setString(7, object.observations());
            statementQuery.setInt(8, object.idReservation());

            LOGGER.log(Level.INFO, "Executing query: {0}", statementQuery);

            statementQuery.executeUpdate();
            connection.commit();

            rawRecord.setServerMessage("Reservation updated successfully");
            rawRecord.setPagination(getList(params, connection));
          }
        }
      }
    } catch(SQLException e) {
      LOGGER.log(Level.SEVERE, "Error updating reservation: {0}", e.getMessage());
      rawRecord.setServerMessage("Error updating reservation");
    }

    return rawRecord;
  }

  @Override
  public RawRecord delete(Integer id, HashMap<String, Object> params) {
    final RawRecord rawRecord = new RawRecord();

    try(
      Connection connection = pool.getConnection();
      SQLClose finish = connection::close;
      PreparedStatement statementQuery = connection.prepareStatement(DELETE_QUERY)
    ) {
      statementQuery.setInt(1, id);

      LOGGER.log(Level.INFO, "Executing query: {0}", statementQuery);

      statementQuery.executeUpdate();
      connection.commit();

      rawRecord.setServerMessage("Reservation deleted successfully");
      rawRecord.setPagination(getList(params, connection));
    } catch(SQLException e) {
      LOGGER.log(Level.SEVERE, "Error deleting reservation: {0}", e.getMessage());
      rawRecord.setServerMessage("Error deleting reservation");
    }

    return rawRecord;
  }

  @Override
  public Reservation getForId(Integer id) {
    try(
      Connection connection = pool.getConnection();
      PreparedStatement statementQuery = connection.prepareStatement(GET_FOR_ID)
    ) {
      statementQuery.setInt(1, id);

      LOGGER.log(Level.INFO, "Executing query: {0}", statementQuery);

      try(ResultSet resultSet = statementQuery.executeQuery()) {
        if (resultSet.next()) {
          final ClientDAO clientDAO = new ClientDAOImplementation(pool);
          final RoomDAO roomDAO = new RoomDAOImplementation(pool);

          return new Reservation(
            resultSet.getInt("eventId"),
            clientDAO.getForId(resultSet.getInt("clientId")),
            null,
            roomDAO.getForId(resultSet.getInt("roomId")),
            resultSet.getDate("date"),
            resultSet.getInt("startHour"),
            resultSet.getInt("endHour"),
            resultSet.getDouble("priceTotal"),
            resultSet.getString("observations")
          );
        }
      }
    } catch(SQLException e) {
      LOGGER.log(Level.SEVERE, "Error getting reservation: {0}", e.getMessage());
    }

    return null;
  }
}
