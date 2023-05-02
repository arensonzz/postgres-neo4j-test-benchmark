import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Session;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.concurrent.locks.ReentrantLock;

public class DataGeneratorThreadItemsAndWorkTypes extends Thread {

    private HashMap<String, String[]> sql_databases;

    private HashMap<String, String> neo4j_settings;

    private int batchExecuteValue = 0;

    private int threadIndex = 0;

    private final int INITIALITEMINDEX;

    private final int INITIALWORKTYPEINDEX;

    int itemIndex = 0;
    int workTypeIndex = 0;

    int itemCount = 0;
    int workTypeCount = 0;

    private ReentrantLock lock;

    public DataGeneratorThreadItemsAndWorkTypes(
            int threadIndex, int batchExecuteValue, HashMap<String, String[]> sql_databases, 
            HashMap<String, String> neo4j_settings, ReentrantLock lock, int itemIndex, int itemCount, 
            int workTypeIndex, int workTypeCount) {
        this.threadIndex = threadIndex;
        this.batchExecuteValue = batchExecuteValue;
        this.itemIndex = itemIndex;
        this.INITIALITEMINDEX = itemIndex;
        this.itemCount = itemCount;
        this.workTypeIndex = workTypeIndex;
        this.INITIALWORKTYPEINDEX = workTypeIndex;
        this.workTypeCount = workTypeCount;
        this.sql_databases = sql_databases;
        this.neo4j_settings = neo4j_settings;
        this.lock = lock;
    }

    public void run() {
        try {
            String neo4j_db_url = neo4j_settings.get("NEO4J_DB_URL");
            String neo4j_username = neo4j_settings.get("NEO4J_USERNAME");
            String neo4j_password = neo4j_settings.get("NEO4J_PASSWORD");
            org.neo4j.driver.Driver driver = GraphDatabase.driver(neo4j_db_url, 
                    AuthTokens.basic(neo4j_username, neo4j_password));
            Session session = driver.session();
            List<HashMap> preparedStatementsList = new ArrayList();
            List<Connection> connectionList = new ArrayList();
            
            for (String db_url : sql_databases.keySet()) {
                String[] db_info = sql_databases.get(db_url);
                String db_driver = db_info[0];
                String db_username = db_info[1];
                String db_password = db_info[2];
                
                Class.forName(db_driver);
                Connection connection = DriverManager.getConnection(db_url + "warehouse", db_username, db_password);
                
                connectionList.add(connection);
                
                PreparedStatement item = connection.prepareStatement(
                        "INSERT INTO item (id, name, balance, unit, purchaseprice, vat, removed) VALUES (?,?,?,?,?,?,?)");
                PreparedStatement workType = connection.prepareStatement(
                        "INSERT INTO worktype (id, name, price) VALUES (?, ?, ?)");
                HashMap<String, PreparedStatement> preparedStatements = new HashMap<String, PreparedStatement>();
                
                preparedStatements.put("item", item);
                preparedStatements.put("worktype", workType);
                preparedStatementsList.add(preparedStatements);
            }
            for (int iterator = 0; iterator < itemCount; iterator++) {
                insertItems(iterator, batchExecuteValue, session, preparedStatementsList);
                itemIndex++;
            }
            for (int iterator = 0; iterator < workTypeCount; iterator++) {
                insertWorkTypes(iterator, batchExecuteValue, session, preparedStatementsList);
                workTypeIndex++;
            }
            for (Connection connection : connectionList) {
                connection.close();
            }
            session.close();
            driver.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void writeToNeo4J(Session session, String cypherQuery) throws SQLException {
        session.writeTransaction(tx -> tx.run(cypherQuery));
    }

    public void insertItems(int iterator, int batchExecuteValue, Session session, List<HashMap> preparedStatementsList) 
            throws SQLException {
        PreparedStatement item;
        System.out.println("threadIndex: " + threadIndex + " itemIndex: " + itemIndex);
        Random r = new Random(itemIndex);
        int balance = r.nextInt(100);
        
        r.setSeed(itemIndex);
        float purchaseprice = r.nextFloat() + r.nextInt(100);
        r.setSeed(itemIndex);
        int vat = r.nextInt(50);
        r.setSeed(itemIndex);
        boolean removed = r.nextBoolean();
        
        if (itemIndex % 2 == 0) {
            r.setSeed(itemIndex);
            r = new Random(itemIndex);
            int x = r.nextInt(10);
            r.setSeed(itemIndex + 1);
            int y = r.nextInt(10);
            r.setSeed(itemIndex + 2);
            int size = r.nextInt(10);
            String itemName = "MMJ " + x + "X" + y + "," + size + "MM²CABLE";
            for (HashMap<String, PreparedStatement> preparedStatements : preparedStatementsList) {
                item = preparedStatements.get("item");
                item.setInt(1, itemIndex);
                item.setString(2, itemName);
                item.setInt(3, balance);
                item.setString(4, "m");
                item.setFloat(5, purchaseprice);
                item.setInt(6, vat);
                item.setBoolean(7, removed);
                item.addBatch();
            }
            session.run("CREATE (v:item {itemId: " + itemIndex + ", name: \"" + itemName + "\", balance:" + balance + ", unit:\"m\", purchaseprice:" + purchaseprice + ", vat:" + vat + ", removed:" + removed + "})");
        } else if (itemIndex % 3 == 0) {
            r.setSeed(itemIndex);
            int ground = r.nextInt(10);
            String itemName = "SOCKET " + ground + "-GROUND OL JUSSI";
            for (HashMap<String, PreparedStatement> preparedStatements : preparedStatementsList) {
                item = preparedStatements.get("item");
                item.setInt(1, itemIndex);
                item.setString(2, itemName);
                item.setInt(3, balance);
                item.setString(4, "pcs");
                item.setFloat(5, purchaseprice);
                item.setInt(6, vat);
                item.setBoolean(7, removed);
                item.addBatch();
            }
            session.run("CREATE (v:item {itemId: " + itemIndex + ", name:\"" + itemName + "\", balance:" + balance + ", unit:\"pcs\", purchaseprice:" + purchaseprice + ", vat:" + vat + ", removed:" + removed + "})");
        } else if (itemIndex % 5 == 0) {
            r.setSeed(itemIndex);
            int spiral1 = r.nextInt(10);
            r.setSeed(itemIndex + 1);
            int spiral2 = r.nextInt(10);
            r.setSeed(itemIndex + 2);
            int spiral3 = r.nextInt(100);
            String itemName = "BINDING SPIRAL " + spiral1 + "," + spiral2 + "-" + spiral3 + "MM INVISIBLE";
            for (HashMap<String, PreparedStatement> preparedStatements : preparedStatementsList) {
                item = preparedStatements.get("item");
                item.setInt(1, itemIndex);
                item.setString(2, itemName);
                item.setInt(3, balance);
                item.setString(4, "pcs");
                item.setFloat(5, purchaseprice);
                item.setInt(6, vat);
                item.setBoolean(7, removed);
                item.addBatch();
            }
            session.run("CREATE (v:item {itemId: " + itemIndex + ", name:\"" + itemName + "\", balance:" + balance + ", unit:\"pcs\", purchaseprice:" + purchaseprice + ", vat:" + vat + ", removed:" + removed + "})");
        } else {
            r.setSeed(itemIndex);
            int parts = r.nextInt(10);
            String itemName = "SOCKET CORNER MODEL " + parts + "-PARTS";
            for (HashMap<String, PreparedStatement> preparedStatements : preparedStatementsList) {
                item = preparedStatements.get("item");
                item.setInt(1, itemIndex);
                item.setString(2, itemName);
                item.setInt(3, balance);
                item.setString(4, "'pcs'");
                item.setFloat(5, purchaseprice);
                item.setInt(6, vat);
                item.setBoolean(7, removed);
                item.addBatch();
            }
            session.run("CREATE (v:item {itemId: " + itemIndex + ", name:\"" + itemName + "\", balance:" + balance + ", unit:\"pcs\", purchaseprice:" + purchaseprice + ", vat:" + vat + ", removed:" + removed + "})");
        }
        if (iterator % batchExecuteValue == 0 || iterator == itemCount - 1) {
            for (HashMap<String, PreparedStatement> preparedStatements : preparedStatementsList) {
                item = preparedStatements.get("item");
                item.executeBatch();
            }
        }
    }

    public void insertWorkTypes(int iterator, int batchExecuteValue, Session session, List<HashMap> preparedStatementsList) 
            throws SQLException {
        PreparedStatement workType;
        Random r = new Random(workTypeIndex);
        int price = r.nextInt(100);
        
        for (HashMap<String, PreparedStatement> preparedStatements : preparedStatementsList) {
            workType = preparedStatements.get("worktype");
            workType.setInt(1, workTypeIndex);
            workType.setInt(3, price);
        }
        if (workTypeIndex % 2 == 0) {
            for (HashMap<String, PreparedStatement> preparedStatements : preparedStatementsList) {
                workType = preparedStatements.get("worktype");
                workType.setString(2, "design");
            }
            session.run("CREATE (wt:worktype {worktypeId: " + workTypeIndex + ", name:\"design\", price:" + price + "})");
        } else if (workTypeIndex % 3 == 0) {
            for (HashMap<String, PreparedStatement> preparedStatements : preparedStatementsList) {
                workType = preparedStatements.get("worktype");
                workType.setString(2, "work");
            }
            session.run("CREATE (wt:worktype {worktypeId: " + workTypeIndex + ", name:\"work\", price:" + price + "})");
        } else {
            for (HashMap<String, PreparedStatement> preparedStatements : preparedStatementsList) {
                workType = preparedStatements.get("worktype");
                workType.setString(2, "supporting work");
            }
            session.run("CREATE (wt:worktype {worktypeId: " + workTypeIndex + ", name:\"supporting work\", price:" + price + "})");
        }
        for (HashMap<String, PreparedStatement> preparedStatements : preparedStatementsList) {
            workType = preparedStatements.get("worktype");
            workType.addBatch();
        }
        if (iterator % batchExecuteValue == 0 || iterator == workTypeCount - 1) {
            for (HashMap<String, PreparedStatement> preparedStatements : preparedStatementsList) {
                workType = preparedStatements.get("worktype");
                workType.executeBatch();
            }
        }
    }
}