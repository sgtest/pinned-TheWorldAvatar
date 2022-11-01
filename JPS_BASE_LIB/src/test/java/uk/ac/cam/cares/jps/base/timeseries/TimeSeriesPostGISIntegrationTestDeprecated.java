package uk.ac.cam.cares.jps.base.timeseries;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.postgis.Point;
import org.postgis.Polygon;

import uk.ac.cam.cares.jps.base.exception.JPSRuntimeException;

public class TimeSeriesPostGISIntegrationTestDeprecated {
    // Using special testcontainers URL that will spin up a Docker container when accessed by a driver
    // (see: https://www.testcontainers.org/modules/databases/jdbc/). Note: requires Docker to be installed!
    private static final String dbURL = "jdbc:tc:postgis:14-3.2:///timeseries";
    private static final String user = "postgres";
    private static final String password = "postgres";

    private static Connection conn;
    private static DSLContext context;

    private String tableName;

    // RDB client
    private TimeSeriesRDBClient<Integer> tsClient;

    @BeforeClass
    // Connect to the database before any test (will spin up the Docker container for the database)
    public static void connect() throws SQLException, ClassNotFoundException {
        // Load required driver
        Class.forName("org.postgresql.Driver");
        // Connect to DB
        conn = DriverManager.getConnection(dbURL, user, password);
        context = DSL.using(conn, SQLDialect.POSTGRES);
    }

    @Before
    public void initialiseRDBClientAndTable() {
        tsClient = new TimeSeriesRDBClient<>(Integer.class);
        tsClient.setRdbURL(dbURL);
        tsClient.setRdbUser(user);
        tsClient.setRdbPassword(password);

        tableName = tsClient.initTimeSeriesTable(Arrays.asList("http://data1"), Arrays.asList(Point.class), "http://ts1", 4326);
    }

    @After
    public void clearDatabase() {
        tsClient.deleteAll();
    }

    /**
     * simple test that checks the number of columns is correct
     */
    @Test
    public void testInitTimeSeriesTable() {
        // 1 for time column and 1 for the geometry column
        Assert.assertTrue(context.meta().getTables(tableName).get(0).fields().length == 2);
    }
    /**
     * uploading a geometry with the wrong srid will throw an exception
     * the column was initialised with 4326 and this function tries to upload a point with 4325
     */
    @Test
    public void testWrongSRID() {
        // a dummy point
        Point point = new Point();
        point.setX(1);
        point.setY(1);
        point.setSrid(4325);
        List<List<?>> values = new ArrayList<>();
        values.add(Arrays.asList(point));

        TimeSeries<Integer> tsUpload = new TimeSeries<Integer>(Arrays.asList(1), Arrays.asList("http://data1"), values);

        // upload to database
        Assert.assertThrows(JPSRuntimeException.class, () -> tsClient.addTimeSeriesData(Arrays.asList(tsUpload)));;
    }

    /**
     * uploading the wrong geometry type will throw an exception
     * @throws SQLException
     */
    @Test
    public void testWrongGeometry() throws SQLException {
        Polygon polygon = new Polygon("POLYGON ((1 1, 2 1, 2 2, 1 2, 1 1))");
        polygon.setSrid(4326);

        List<List<?>> values = new ArrayList<>();
        values.add(Arrays.asList(polygon));

        TimeSeries<Integer> tsUpload = new TimeSeries<Integer>(Arrays.asList(1), Arrays.asList("http://data1"), values);
        // upload to database
        Assert.assertThrows(JPSRuntimeException.class, () -> tsClient.addTimeSeriesData(Arrays.asList(tsUpload)));;
    }

    /**
     * uploads dummy data, queries it and checks if it's the same
     */
    @Test
    public void testAddTimeSeriesData() {
        Point point = new Point();
        point.setX(1);
        point.setY(1);
        point.setSrid(4326);
        List<List<?>> values = new ArrayList<>();
        values.add(Arrays.asList(point));

        // upload data
        TimeSeries<Integer> tsUpload = new TimeSeries<Integer>(Arrays.asList(1), Arrays.asList("http://data1"), values);
        tsClient.addTimeSeriesData(Arrays.asList(tsUpload));

        // query and check if it's the same
        Point queriedPoint = tsClient.getTimeSeries(Arrays.asList("http://data1")).getValuesAsPoint("http://data1").get(0);
        Assert.assertTrue(queriedPoint.equals(point));
    }

    @AfterClass
    public static void disconnect() throws SQLException {
        conn.close();
    }
}