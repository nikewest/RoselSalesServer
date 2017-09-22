package modules.data;

import java.sql.Connection;
import java.sql.PreparedStatement;
import modules.serverlogic.DeviceInfo;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import modules.serverlogic.RoselServerModel;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.StatementCallback;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component("serverDAO")
public class RoselServerDAOjdbc implements RoselServerDAO {
    
    private static final Logger LOG = Logger.getLogger(RoselServerModel.class.getName());
    
    @Autowired
    private JdbcTemplate jdbcTemplate;

    public RoselServerDAOjdbc() {
    }

    @Override
    public String getClientName(long clientId) {
        String sql = "SELECT name FROM CLIENTS WHERE _id = ?";
        return jdbcTemplate.queryForObject(sql, String.class, clientId);
    }

    @Override
    public ArrayList<String> getUpdates(long device_id, ArrayList<String> updateRequestInfo) {

        ArrayList<String> res = new ArrayList<>();
        RoselUpdateStructure responseUpdateStructure;
        RoselUpdateStructure updateStructure;
        UpdateStructureFactory updateStructureFactory = new RoselUpdateStructureFactory(new RoselUpdateItemFactory());

        for (String string : updateRequestInfo) {
            updateStructure = updateStructureFactory.fillFromJSONString(string);
            switch (updateStructure.getTableName()) {
                case "CLIENTS":
                    //CLIENTS updates
                    responseUpdateStructure = jdbcTemplate.query(getClientsUpdatesQuery(updateStructure.getUpdateVersion(), device_id), new RoselUpdateResultSetExtractor("CLIENTS"));
                    if (!responseUpdateStructure.isEmpty()) {
                        res.add(responseUpdateStructure.toJSONObject().toJSONString());
                    }
                    break;
                case "PRODUCTS":
                    //PRODUCTS updates
                    responseUpdateStructure = jdbcTemplate.query(getProductsUpdatesQuery(updateStructure.getUpdateVersion()), new RoselUpdateResultSetExtractor("PRODUCTS"));
                    if (!responseUpdateStructure.isEmpty()) {
                        res.add(responseUpdateStructure.toJSONObject().toJSONString());
                    }
                    break;

                case "ADDRESSES":
                    //ADDRESSES updates
                    responseUpdateStructure = jdbcTemplate.query(getAddressesUpdatesQuery(updateStructure.getUpdateVersion(), device_id), new RoselUpdateResultSetExtractor("ADDRESSES"));
                    if (!responseUpdateStructure.isEmpty()) {
                        res.add(responseUpdateStructure.toJSONObject().toJSONString());
                    }
                    break;
                case "PRICES":
                    //PRICES updates
                    responseUpdateStructure = jdbcTemplate.query(getPricesUpdatesQuery(updateStructure.getUpdateVersion(), device_id), new RoselUpdateResultSetExtractor("PRICES"));
                    if (!responseUpdateStructure.isEmpty()) {
                        res.add(responseUpdateStructure.toJSONObject().toJSONString());
                    }
                    break;
                case "STOCK":
                    //STOCK updates
                    responseUpdateStructure = jdbcTemplate.query(getStockUpdatesQuery(), new RoselUpdateResultSetExtractor("STOCK"));
                    if (!responseUpdateStructure.isEmpty()) {
                        res.add(responseUpdateStructure.toJSONObject().toJSONString());
                    }
                    break;
            }
        }

        return res;
    }

    private static String getClientsUpdatesQuery(long version, long device_id) {
        return "SELECT temp.version, temp.action, C.* FROM "
                + "(SELECT "
                + "	U.item_id,"
                + "	MAX(U._id) AS version,"
                + "	MIN(U.action) AS action "
                + "FROM UPDATES AS U WHERE " + version + " < U._id AND U.table_name = 'CLIENTS' "
                + "GROUP BY U.item_id) AS temp "
                + "     INNER JOIN DEVICES AS D ON D._id = " + device_id
                + "     INNER JOIN CLIENTS AS C ON C._id = temp.item_id AND C.manager_id = D.manager_id"
                + " ORDER BY temp.version";
    }

    private static String getProductsUpdatesQuery(long version) {
        return "SELECT temp.version, temp.action, P.* FROM "
                + "(SELECT "
                + "	U.item_id,"
                + "	MAX(U._id) AS version,"
                + "	MIN(U.action) AS action "                
                + "FROM UPDATES AS U WHERE " + version + " < U._id AND U.table_name = 'PRODUCTS' "
                + "GROUP BY U.item_id) AS temp "
                + "     INNER JOIN PRODUCTS AS P ON P._id = temp.item_id"
                + " ORDER BY temp.version";
    }

    private static String getAddressesUpdatesQuery(long version, long device_id) {
        return "SELECT temp.version, temp.action, A.* FROM "
                + "(SELECT "
                + "	U.item_id,"
                + "	MAX(U._id) AS version,"
                + "	MIN(U.action) AS action "                 
                + "FROM UPDATES AS U WHERE " + version + " < U._id AND U.table_name = 'ADDRESSES' "
                + "GROUP BY U.item_id) AS temp "
                + "     INNER JOIN ADDRESSES AS A ON A._id = temp.item_id "
                + "     INNER JOIN DEVICES AS D ON D._id = " + device_id
                + "     INNER JOIN CLIENTS AS C ON A.client_id = C._id AND C.manager_id = D.manager_id"
                + " ORDER BY temp.version";
    }

    private static String getPricesUpdatesQuery(long version, long device_id) {
        return "SELECT temp.version, temp.action, P.* FROM "
                + "(SELECT "
                + "	U.item_id,"
                + "	MAX(U._id) AS version,"
                + "	MIN(U.action) AS action "                
                + "FROM UPDATES AS U WHERE " + version + " < U._id AND U.table_name = 'PRICES' "
                + "GROUP BY U.item_id) AS temp "
                + "       INNER JOIN PRICES AS P ON P._id = temp.item_id "
                + "       INNER JOIN DEVICES AS D ON D._id = " + device_id
                + "       INNER JOIN CLIENTS AS C ON P.client_id = C._id AND C.manager_id = D.manager_id"
                + " ORDER BY temp.version";
    }

    private String getStockUpdatesQuery() {
        return "SELECT 0 as version, 1 as action, S.* FROM STOCK AS S";
    }

    @Override
    public DeviceInfo getDeviceInfo(String device_id) {
        List<DeviceInfo> res = jdbcTemplate.query(getDeviceInfoQuery(device_id), new DeviceInfoRowMapper());
        if (res.isEmpty()) {
            return initializeDevice(device_id);
        } else {
            return res.get(0);
        }
    }

    private static String getDeviceInfoQuery(String device_id) {
        return "SELECT _id, device_id, name, manager_id, CASE ISNULL(manager_id,0) WHEN 0 THEN 0 ELSE 1 END AS confirmed FROM DEVICES WHERE device_id = '" + device_id + "'";
    }

    private static String getNewDeviceQuery(String device_id) {
        return "IF NOT EXISTS (SELECT device_id from DEVICES WHERE device_id = '" + device_id + "') "
                + "INSERT INTO DEVICES (device_id) VALUES ('" + device_id + "');";
    }

    public DeviceInfo initializeDevice(String device_id) {
        
        jdbcTemplate.execute(getNewDeviceQuery(device_id));
        List<DeviceInfo> res = jdbcTemplate.query(getDeviceInfoQuery(device_id), new DeviceInfoRowMapper());
        DeviceInfo deviceInfo = res.get(0);
        
        return deviceInfo;
        
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void postOrdersFromJSON(long device_id, ArrayList<String> ordersInJSON) {

        String orderDate;
        String shippingDate;
        long order_id;
        String insertQuery;

        JSONParser parser = new JSONParser();
        org.json.simple.JSONObject jsonObject;
        JSONArray lines;
        JSONObject orderLine;

        try {
            for (String jsonString : ordersInJSON) {
                jsonObject = (JSONObject) parser.parse(jsonString);
                if (jsonObject.get("order_date") == null) {
                    orderDate = "null";
                } else {
                    orderDate = "'" + jsonObject.get("order_date").toString() + "'";
                }
                if (jsonObject.get("shipping_date") == null) {
                    shippingDate = "null";
                } else {
                    shippingDate = "'" + jsonObject.get("shipping_date").toString() + "'";
                }
                insertQuery = "INSERT INTO ORDERS (device_id, client_id, order_date, shipping_date, sum, comment, address_id) "
                        + "VALUES ("
                        + device_id + ", "
                        + jsonObject.get("client_id") + ", "
                        + orderDate + ", "
                        + shippingDate + ", "
                        + "ROUND(" + jsonObject.get("sum") + ",2), '"
                        + jsonObject.get("comment") + "', "
                        + jsonObject.get("address_id") + ");";
                GeneratedKeyHolder generatedKeyHolder = new GeneratedKeyHolder();
                jdbcTemplate.update(new OrdersPreparedStatementCreator(insertQuery), generatedKeyHolder);
                order_id = generatedKeyHolder.getKey().longValue();

                lines = (JSONArray) jsonObject.get("lines");
                int len = lines.size();
                for (int i = 0; i < len; i++) {
                    orderLine = (JSONObject) lines.get(i);
                    insertQuery = "INSERT INTO ORDERLINES (order_id, product_id, quantity, price, sum) VALUES (" + String.format("%d", order_id) + ", " + orderLine.get("product_id").toString() + ", " + orderLine.get("quantity").toString() + "," + orderLine.get("price").toString() + ", " + orderLine.get("sum").toString() + ");";
                    jdbcTemplate.execute(insertQuery);
                }

            }
        } catch (ParseException pex) {
            LOG.log(Level.SEVERE, pex.getMessage());
        }
    }

    private final class OrdersPreparedStatementCreator implements PreparedStatementCreator {

        String sqlQuery;

        public OrdersPreparedStatementCreator(String sqlQuery) {
            this.sqlQuery = sqlQuery;
        }

        @Override
        public PreparedStatement createPreparedStatement(Connection cnctn) throws SQLException {
            PreparedStatement stmt = cnctn.prepareStatement(sqlQuery, Statement.RETURN_GENERATED_KEYS);
            return stmt;
        }
    }
    
    @Override
    public void initializeDataStructure() {
       
        // DELETE TABLES
        jdbcTemplate.execute("IF OBJECT_ID('STOCK', 'U') IS NOT NULL DROP TABLE STOCK;");
        jdbcTemplate.execute("IF OBJECT_ID('ORDERLINES', 'U') IS NOT NULL DROP TABLE ORDERLINES;");
        jdbcTemplate.execute("IF OBJECT_ID('ORDERS', 'U') IS NOT NULL DROP TABLE ORDERS;");
        jdbcTemplate.execute("IF OBJECT_ID('PRICES', 'U') IS NOT NULL DROP TABLE PRICES;");
        jdbcTemplate.execute("IF OBJECT_ID('ADDRESSES', 'U') IS NOT NULL DROP TABLE ADDRESSES");
        jdbcTemplate.execute("IF OBJECT_ID('CLIENTS', 'U') IS NOT NULL DROP TABLE CLIENTS;");
        jdbcTemplate.execute("IF OBJECT_ID('PRODUCTS', 'U') IS NOT NULL DROP TABLE PRODUCTS;");
        jdbcTemplate.execute("IF OBJECT_ID('DEVICES', 'U') IS NOT NULL DROP TABLE DEVICES;");
        jdbcTemplate.execute("IF OBJECT_ID('MANAGERS', 'U') IS NOT NULL DROP TABLE MANAGERS;");
        jdbcTemplate.execute("IF OBJECT_ID('UPDATES', 'U') IS NOT NULL DROP TABLE UPDATES");

        //CREATE TABLES
        jdbcTemplate.execute("CREATE TABLE MANAGERS (\n"
                + "  _id       integer NOT NULL PRIMARY KEY IDENTITY(1,1),\n"
                + "  rosel_id  nvarchar(100),\n"
                + "  /* Keys */\n"
                + "  UNIQUE (rosel_id)\n"
                + ");");
        jdbcTemplate.execute("CREATE TABLE CLIENTS (\n"
                + "  _id        integer PRIMARY KEY IDENTITY(1,1),\n"
                + "  rosel_id   nchar(9),\n"
                + "  name       text,\n"
                + "  address    text,\n"
                + "  manager_id integer,\n"
                + "  /* Keys */\n"
                + "  UNIQUE (rosel_id),\n"
                + "  /* Foreign keys */\n"
                + "  CONSTRAINT CLIENTS_Foreign_key01\n"
                + "    FOREIGN KEY (manager_id)\n"
                + "    REFERENCES MANAGERS(_id)\n"
                + ");");
        jdbcTemplate.execute("CREATE TABLE ADDRESSES (\n"
                + "  _id       integer PRIMARY KEY IDENTITY(1,1),\n"
                + "  rosel_id  nchar(11),\n"
                + "  client_id integer,\n"
                + "  address   text,\n"
                + "  /* Keys */\n"
                + "  UNIQUE (rosel_id),\n"
                + "  /* Foreign keys */\n"
                + "  CONSTRAINT ADDRESSES_Foreign_key01\n"
                + "    FOREIGN KEY (client_id)\n"
                + "    REFERENCES CLIENTS(_id), \n"
                + ");");
        jdbcTemplate.execute("CREATE TABLE PRODUCTS (\n"
                + "  _id       integer PRIMARY KEY IDENTITY(1,1),\n"
                + "  rosel_id  nchar(11),\n"
                + "  code      text,\n"
                + "  name      text,\n"
                + "  isgroup   integer,\n"
                + "  group_id  text,\n"
                + "  show      integer,\n"
                + "  /* Keys */\n"
                + "  UNIQUE (rosel_id)\n"
                + ");");
        jdbcTemplate.execute("CREATE TABLE DEVICES (\n"
                + "  _id         integer PRIMARY KEY IDENTITY(1,1),\n"
                + "  device_id   nvarchar(50),\n"
                + "  name        text,\n"
                + "  manager_id  integer,\n"
                + "  /* Keys */\n"
                + "  UNIQUE (device_id),\n"
                + "  /* Foreign keys */\n"
                + "  CONSTRAINT DEVICES_Foreign_key01\n"
                + "    FOREIGN KEY (manager_id)\n"
                + "    REFERENCES MANAGERS(_id)\n"
                + ");");
        jdbcTemplate.execute("CREATE TABLE ORDERS (\n"
                + "  _id         integer PRIMARY KEY IDENTITY(1,1),\n"
                + "  client_id   integer,\n"
                + "  device_id   integer,\n"
                + "  rosel_id    nchar(11),\n"
                + "  address_id  integer,\n"
                + "  order_date  text,\n"
                + "  shipping_date  text,\n"
                + "  comment  text,\n"
                + "  \"sum\"       real,\n"
                + "  /* Foreign keys */\n"
                + "  CONSTRAINT ORDERS_Foreign_key01\n"
                + "    FOREIGN KEY (client_id)\n"
                + "    REFERENCES CLIENTS(_id), \n"
                + "  CONSTRAINT ORDERS_Foreign_key02\n"
                + "    FOREIGN KEY (device_id)\n"
                + "    REFERENCES DEVICES(_id),\n"
                + "  CONSTRAINT ORDERS_Foreign_key03\n"
                + "    FOREIGN KEY (address_id)\n"
                + "    REFERENCES ADDRESSES(_id)\n"
                + ");");
        jdbcTemplate.execute("CREATE TABLE ORDERLINES (\n"
                + "  _id         integer PRIMARY KEY IDENTITY(1,1),\n"
                + "  order_id    integer,\n"
                + "  product_id  integer,\n"
                + "  quantity    integer,\n"
                + "  price       real,\n"
                + "  \"sum\"       real,\n"
                + "  /* Foreign keys */\n"
                + "  CONSTRAINT ORDERLINES_Foreign_key01\n"
                + "    FOREIGN KEY (product_id)\n"
                + "    REFERENCES PRODUCTS(_id), \n"
                + "  CONSTRAINT ORDERLINES_Foreign_key02\n"
                + "    FOREIGN KEY (order_id)\n"
                + "    REFERENCES ORDERS(_id)\n"
                + ");");
        jdbcTemplate.execute("CREATE TABLE PRICES (\n"
                + "  _id         integer PRIMARY KEY IDENTITY(1,1),\n"
                + "  product_id  integer,\n"
                + "  client_id   integer,\n"
                + "  price       real,\n"
                + "  /* Keys */\n"
                + "  UNIQUE (product_id, client_id),\n"
                + "  /* Foreign keys */\n"
                + "  CONSTRAINT PRICES_Foreign_key02\n"
                + "    FOREIGN KEY (product_id)\n"
                + "    REFERENCES PRODUCTS(_id), \n"
                + "  CONSTRAINT PRICES_Foreign_key01\n"
                + "    FOREIGN KEY (client_id)\n"
                + "    REFERENCES CLIENTS(_id)\n"
                + ");");
        jdbcTemplate.execute("CREATE TABLE STOCK (\n"
                + "  _id         integer PRIMARY KEY IDENTITY(1,1),\n"
                + "  product_id  integer,\n"
                + "  quantity    real,\n"
                + "  /* Keys */\n"
                + "  UNIQUE (product_id),\n"
                + "  /* Foreign keys */\n"
                + "  CONSTRAINT STOCK_Foreign_key01\n"
                + "    FOREIGN KEY (product_id)\n"
                + "    REFERENCES PRODUCTS(_id)\n"
                + ");");
        jdbcTemplate.execute("CREATE TABLE UPDATES (\n"
                + "  _id         integer NOT NULL PRIMARY KEY IDENTITY(1,1),\n"
                + "  item_id     integer NOT NULL,\n"
                + "  table_name  nvarchar(20),\n"
                + "  \"action\"    integer,\n"
                + "  version     nchar(11),\n"
                + ");");
    }

    private static final class RoselUpdateResultSetExtractor implements ResultSetExtractor<RoselUpdateStructure> {

        private String tableName;

        public RoselUpdateResultSetExtractor(String tableName) {
            this.tableName = tableName;
        }

        @Override
        public RoselUpdateStructure extractData(ResultSet rs) throws SQLException, DataAccessException {
            RoselUpdateItemFactory factory = new RoselUpdateItemFactory();
            RoselUpdateStructure updateStructure = new RoselUpdateStructure(tableName);
            ArrayList<RoselUpdateItem> res = new ArrayList<>();
            long curVersion = 0;
            while (rs.next()) {
                res.add(factory.fillFromResultSet(rs, 2));
                curVersion = rs.getLong("version");
            }
            updateStructure.setUpdateItems(res);                                    
            updateStructure.setUpdateVersion(curVersion);
            
            return updateStructure;
        }
    }

    private static final class DeviceInfoRowMapper implements RowMapper<DeviceInfo> {

        @Override
        public DeviceInfo mapRow(ResultSet rs, int i) throws SQLException {
            DeviceInfo di = new DeviceInfo();
            di.setInnerId(rs.getLong("_id"));
            di.setDevice_id(rs.getString("device_id"));
            di.setConfirmed(rs.getBoolean("confirmed"));
            return di;
        }

    }
}