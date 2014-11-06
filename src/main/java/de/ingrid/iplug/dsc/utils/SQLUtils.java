/**
 * Copyright (c) 2014 wemove GmbH
 * Licensed under the EUPL V.1.1
 *
 * This Software is provided to You under the terms of the European
 * Union Public License (the "EUPL") version 1.1 as published by the
 * European Union. Any use of this Software, other than as authorized
 * under this License is strictly prohibited (to the extent such use
 * is covered by a right of the copyright holder of this Software).
 *
 * This Software is provided under the License on an "AS IS" basis and
 * without warranties of any kind concerning the Software, including
 * without limitation merchantability, fitness for a particular purpose,
 * absence of defects or errors, accuracy, and non-infringement of
 * intellectual property rights other than copyright. This disclaimer
 * of warranty is an essential part of the License and a condition for
 * the grant of any rights to this Software.
 *
 * For more  details, see <http://joinup.ec.europa.eu/software/page/eupl>
 */
/**
 * 
 */
package de.ingrid.iplug.dsc.utils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

/**
 * Helper class encapsulating SQL access via JDBC connection (e.g. used in
 * mapping script). Must be instantiated to be thread safe.
 * 
 * @author Martin
 */
public class SQLUtils {

    private static final Logger log = Logger.getLogger(SQLUtils.class);

    private Connection connection = null;

    public SQLUtils(Connection connection) {
        this.connection = connection;
    }

    public Connection getConnection() {
    	return connection;
    }

    /**
     * Executes a SQL and returns the first record result.
     * 
     * @param sqlStr
     *            The SQL to execute.
     * @return A map with column names as keys and column values as String (can
     *         be null)
     * @throws SQLException
     */
    public Map<String, String> first(String sqlStr) throws SQLException {
        List<Map<String, String>> results = all(sqlStr);
        if (results.size() > 0) {
            return results.get(0);
        } else {
            return null;
        }
    }

    /**
     * Executes a SQL and returns the first record result.
     * 
     * @param sqlStr
     *            The SQL to execute.
     * @param sqlParams
     *            The params to be set on SQL string. NOTICE: NO null params !
     * @return A map with column names as keys and column values as String (can
     *         be null)
     * @throws SQLException
     */
    public Map<String, String> first(String sqlStr, Object[] sqlParams) throws SQLException {
        List<Map<String, String>> results = all(sqlStr, sqlParams);
        if (results.size() > 0) {
            return results.get(0);
        } else {
            return null;
        }
    }

    /**
     * Executes a SQL and returns a list with records results
     * 
     * @param sqlStr
     *            the sql to execute
     * @return list of maps, one per row, with column names as keys and column
     *         values as String (can be null)
     * @throws SQLException
     */
    public List<Map<String, String>> all(String sqlStr) throws SQLException {
        return all(sqlStr, null);
    }

    /**
     * Executes a SQL and returns a list with records results
     * 
     * @param sqlStr
     *            the sql to execute
     * @param sqlParams
     *            the params to be set on SQL string. NOTICE: NO null params !
     * @return list of maps, one per row, with column names IN LOWER CASE (!!!)
     *         as keys and column values as String (can be null)
     * @throws SQLException
     */
    public List<Map<String, String>> all(String sqlStr, Object[] sqlParams) throws SQLException {
        if (log.isDebugEnabled()) {
            log.debug("Execute sql: '" + sqlStr + "' with parameters: " + Arrays.toString(sqlParams));
        }
        PreparedStatement ps = null;
        try {
            ps = connection.prepareStatement(sqlStr);
            if (sqlParams != null) {
                for (int i = 0; i < sqlParams.length; i++) {
                    if (sqlParams[i] != null) {
                        ps.setString(i + 1, sqlParams[i].toString());
                    } else {
                        log.error("Prepared statement argument " + i + " in null for: " + sqlStr);
                        throw new SQLException("Prepared statement argument " + i + " is null for: " + sqlStr);
                    }
                }
            }

            ResultSet rs = ps.executeQuery();
            List<Map<String, String>> result = toList(rs);
            rs.close();

            return result;

        } catch (SQLException ex) {
            log.error("Error fetching all records from SQL. Sql: " + sqlStr + ", sqlParams: " + Arrays.toString(sqlParams)
                    + ", Exception: " + ex);
            throw ex;
        } finally {
            if (ps != null) {
                ps.close();
            }
        }
    }

    /**
     * Helper method that converts a ResultSet into a list of maps, one per row
     * 
     * @param query
     *            ResultSet
     * @return list of maps, one per column row, with column names as keys and
     *         column values as String (can be null)
     * @throws SQLException
     *             if the connection fails
     */
    private List<Map<String, String>> toList(ResultSet rs) throws SQLException {
        List<String> wantedColumnNames = getColumnNames(rs);

        return toList(rs, wantedColumnNames);
    }

    /**
     * Helper method that maps a ResultSet into a list of maps, one per row.
     * NOTICE: Calls trim() to remove whitespaces from values
     * 
     * @param query
     *            ResultSet
     * @param list
     *            of columns names to include in the result map
     * @return list of maps, one per column row, with column names as keys and
     *         column values as String (can be null)
     * @throws SQLException
     *             if the connection fails
     */
    private List<Map<String, String>> toList(ResultSet rs, List<String> wantedColumnNames) throws SQLException {
        List<Map<String, String>> rows = new ArrayList<Map<String, String>>();

        while (rs.next()) {
            Map<String, String> row = new LinkedHashMap<String, String>();

            for (String columnName : wantedColumnNames) {
                String value = rs.getString(columnName);
                if (value != null) {
                    value = value.trim();
                }
                row.put(columnName.toLowerCase(), value);
            }

            rows.add(row);
        }

        return rows;
    }

    /**
     * Return all column names as a list of strings
     * 
     * @param database
     *            query result set
     * @return list of column name strings
     * @throws SQLException
     *             if the query fails
     */
    private final List<String> getColumnNames(ResultSet rs) throws SQLException {
        List<String> columnNames = new ArrayList<String>();

        ResultSetMetaData meta = rs.getMetaData();
        int numColumns = meta.getColumnCount();
        for (int i = 1; i <= numColumns; ++i) {
            columnNames.add(meta.getColumnName(i));
        }

        return columnNames;
    }

}
