package iw.framework.data;

import java.sql.Connection;

/**
 * @author Primiano Medugno
 * @since 01/02/2020
 */

public class DataAccessObject {
  protected final DataLayer dataLayer;
  protected final Connection connection;

  public DataAccessObject (DataLayer dataLayer) {
    this.dataLayer = dataLayer;
    this.connection = dataLayer.getConnection();
  }

  protected DataLayer getDataLayer () {
    return dataLayer;
  }

  protected Connection getConnection () {
    return connection;
  }

  public void init () throws DataException {
  }

  public void destroy () throws DataException {
  }
}
