package com.unir.reservations.dao.implementations;

import java.sql.Connection;
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
import com.unir.reservations.dao.SQLClose;
import com.unir.reservations.models.Client;
import com.unir.reservations.utils.Pagination;
import com.unir.reservations.utils.RawRecord;

public class ClientDAOImplementation implements ClientDAO {
  private static final Logger LOGGER = Logger.getLogger(
    ClientDAOImplementation.class.getName()
  );
  private final DataSource pool;

  private static final String GET_LIST_COUNT = """
    SELECT
      COUNT(clientId) as COUNT
    FROM evp_client
    WHERE
      document LIKE CONCAT('%', ?, '%')
  """;
  private static final String GET_LIST_QUERY = """
    SELECT
      clientId,
      document,
      documentType,
      firstName,
      surName,
      phoneNumber,
      mobileNumber,
      email,
    FROM evp_client
    WHERE
      document LIKE CONCAT('%', ?, '%')
  """;
  private static final String ADD_COUNT = """
    SELECT
      COUNT(clientId) as COUNT
    FROM evp_client
    WHERE
      document = ?
  """;
  private static final String ADD_QUERY = """
    INSERT INTO evp_client (
      document, documentType, firstName, surName, phoneNumber, mobileNumber, email
    ) VALUES (?, ?, ?, ?, ?, ?, ?)
  """;
  private static final String UPDATE_COUNT = """
    SELECT
      COUNT(clientId) as COUNT
    FROM evp_client
    WHERE
      document = ? AND clientId != ?
  """;
  private static final String UPDATE_QUERY = """
    UPDATE evp_client
    SET
      document = ?,
      documentType = ?,
      firstName = ?,
      surName = ?,
      phoneNumber = ?,
      mobileNumber = ?,
      email = ?
    WHERE
      clientId = ?
  """;
  private static final String DELETE_QUERY = """
    DELETE FROM evp_client
    WHERE
      clientId = ?
  """;
  private static final String GET_FOR_ID_QUERY = """
    SELECT
      clientId,
      document,
      documentType,
      firstName,
      surName,
      phoneNumber,
      mobileNumber,
      email
    FROM evp_client
    WHERE
      clientId = ?
  """;

  public ClientDAOImplementation(DataSource pool) {
    this.pool = pool;
  }

  @Override
  public Pagination getList(HashMap<String, Object> params, Connection connection) {
    Pagination pagination = new Pagination();
    List<Client> clients = new ArrayList<>();

    try(
      PreparedStatement statementCount = connection.prepareStatement(GET_LIST_COUNT)
    ) {
      statementCount.setString(1, String.valueOf(params.get("FILTER")));

      LOGGER.log(Level.INFO, "Executing query: {0}", statementCount);

      try(
        ResultSet resultSet = statementCount.executeQuery();
        PreparedStatement statementQuery = connection.prepareStatement(GET_LIST_QUERY);
      ) {
        if(resultSet.next()) {
          final int count = resultSet.getInt("COUNT");
          pagination.setFilterCounter(count);

          if (count > 0) {
            statementQuery.setString(1, String.valueOf(params.get("FILTER")));

            LOGGER.log(Level.INFO, "Executing query: {0}", statementQuery);

            try(ResultSet resultSet2 = statementQuery.executeQuery()) {
              while (resultSet2.next()) {
                Client client = new Client(
                  resultSet2.getInt("clientId"),
                  resultSet2.getString("document"),
                  resultSet2.getString("documentType"),
                  resultSet2.getString("firstName"),
                  resultSet2.getString("surName"),
                  resultSet2.getString("phoneNumber"),
                  resultSet2.getString("mobileNumber"),
                  resultSet2.getString("email")
                );

                clients.add(client);
              }
            }
          }
        }
      }
    } catch (SQLException e) {
      LOGGER.log(Level.SEVERE, "Error executing query: {0}", e.getMessage());
    }

    pagination.setData(clients);
    return pagination;
  }

  @Override
  public RawRecord getList(HashMap<String, Object> params) {
    final RawRecord rawRecord = new RawRecord();

    try(Connection connection = this.pool.getConnection()) {
      rawRecord.setPagination(getList(params, connection));
    } catch (SQLException e) {
      LOGGER.log(Level.SEVERE, "Error getting list: {0}", e.getMessage());
      rawRecord.setServerMessage("Error getting list");
    }

    return rawRecord;
  }

  @Override
  public RawRecord add(Client client, HashMap<String, Object> params) {
    final RawRecord rawRecord = new RawRecord();

    try(
      Connection connection = this.pool.getConnection();
      SQLClose finish = connection::rollback;
      PreparedStatement statementCount = connection.prepareStatement(ADD_COUNT);
      ) {
        connection.setAutoCommit(false);

        statementCount.setString(1, client.document());

        LOGGER.log(Level.INFO, "Executing query: {0}", statementCount);

        try(
          ResultSet resultSet = statementCount.executeQuery();
          PreparedStatement statementQuery = connection.prepareStatement(ADD_QUERY);
      ) {
        if (resultSet.next()) {
          final int count = resultSet.getInt("COUNT");
          if (count > 0) {
            rawRecord.setServerMessage("Client already exists");
            return rawRecord;
          }

          statementQuery.setString(1, client.document());
          statementQuery.setString(2, client.documentType());
          statementQuery.setString(3, client.firstName());
          statementQuery.setString(4, client.surName());
          statementQuery.setString(5, client.phoneNumber());
          statementQuery.setString(6, client.mobileNumber());
          statementQuery.setString(7, client.email());

          LOGGER.log(Level.INFO, "Executing query: {0}", statementQuery);

          statementQuery.executeUpdate();
          connection.commit();

          rawRecord.setServerMessage("Client added successfully");
          rawRecord.setPagination(getList(params, connection));
        }
      }
    } catch (SQLException e) {
      LOGGER.log(Level.SEVERE, "Error executing query: {0}", e.getMessage());
      rawRecord.setServerMessage("Error adding client");
    }

    return rawRecord;
  }

  @Override
  public RawRecord update(Client client, HashMap<String, Object> params) {
    final RawRecord rawRecord = new RawRecord();

    try(
      Connection connection = this.pool.getConnection();
      SQLClose finish = connection::rollback;
      PreparedStatement statementCount = connection.prepareStatement(UPDATE_COUNT);
    ) {
      connection.setAutoCommit(false);

      statementCount.setString(1, client.document());
      statementCount.setInt(2, client.idClient());

      LOGGER.log(Level.INFO, "Executing query: {0}", statementCount);

      try(
        ResultSet resultSet = statementCount.executeQuery();
        PreparedStatement statementQuery = connection.prepareStatement(UPDATE_QUERY);
      ) {
        if (resultSet.next()) {
          final int count = resultSet.getInt("COUNT");
          if (count > 0) {
            rawRecord.setServerMessage("Client already exists");
            return rawRecord;
          }

          statementQuery.setString(1, client.document());
          statementQuery.setString(2, client.documentType());
          statementQuery.setString(3, client.firstName());
          statementQuery.setString(4, client.surName());
          statementQuery.setString(5, client.phoneNumber());
          statementQuery.setString(6, client.mobileNumber());
          statementQuery.setString(7, client.email());
          statementQuery.setInt(8, client.idClient());

          LOGGER.log(Level.INFO, "Executing query: {0}", statementQuery);

          statementQuery.executeUpdate();
          connection.commit();

          rawRecord.setServerMessage("Client updated successfully");
          rawRecord.setPagination(getList(params, connection));
        }
      }
    } catch (SQLException e) {
      LOGGER.log(Level.SEVERE, "Error updating client: {0}", e.getMessage());
      rawRecord.setServerMessage("Error updating client");
    }

    return rawRecord;
  }

  @Override
  public RawRecord delete(Integer id, HashMap<String, Object> params) {
    final RawRecord rawRecord = new RawRecord();

    try(
      Connection connection = this.pool.getConnection();
      SQLClose finish = connection::rollback;
      PreparedStatement statementQuery = connection.prepareStatement(DELETE_QUERY);
    ) {
      statementQuery.setInt(1, id);

      LOGGER.log(Level.INFO, "Executing query: {0}", statementQuery);

      statementQuery.executeUpdate();
      connection.commit();

      rawRecord.setServerMessage("Client deleted successfully");
      rawRecord.setPagination(getList(params, connection));
    } catch (SQLException e) {
      LOGGER.log(Level.SEVERE, "Error deleting client: {0}", e.getMessage());
      rawRecord.setServerMessage("Error deleting client");
    }

    return rawRecord;
  }

  @Override
  public Client getForId(Integer id) {
    try (
      Connection connection = this.pool.getConnection();
      PreparedStatement statement = connection.prepareStatement(GET_FOR_ID_QUERY);
    ) {
      statement.setInt(1, id);

      LOGGER.log(Level.INFO, "Executing query: {0}", statement);

      try(ResultSet resultSet = statement.executeQuery()) {
        if (resultSet.next()) {
          return new Client(
            resultSet.getInt("clientId"),
            resultSet.getString("document"),
            resultSet.getString("documentType"),
            resultSet.getString("firstName"),
            resultSet.getString("surName"),
            resultSet.getString("phoneNumber"),
            resultSet.getString("mobileNumber"),
            resultSet.getString("email")
          );
        }
      }
    } catch (SQLException e) {
      LOGGER.log(Level.SEVERE, "Error getting client: {0}", e.getMessage());
    }

    return null;
  }
}
