import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Session;

import java.sql.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

public class DataGeneratorThreadSequentialInvoices extends Thread {

    private HashMap<String, String[]> sql_databases;

    HashMap<String, String> neo4j_settings;

    private int batchExecuteValue = 0;

    private int firstInvoiceIndex = 0;

    private int sequentialInvoiceCount = 0;

    private int threadIndex = 0;
    int customerIndex = 0;
    int invoiceIndex = 0;

    private ReentrantLock lock;

    public DataGeneratorThreadSequentialInvoices(int threadindex, int batchExecuteValue, HashMap<String, String[]> sql_databases, HashMap<String, String> neo4j_settings, ReentrantLock lock, int sequentialInvoiceCount, int customerIndex, int invoiceIndex, int firstInvoiceIndex) {
        this.threadIndex = threadindex;
        this.batchExecuteValue = batchExecuteValue;
        this.sequentialInvoiceCount = sequentialInvoiceCount;
        this.customerIndex = customerIndex;
        this.invoiceIndex = invoiceIndex;
        this.firstInvoiceIndex = firstInvoiceIndex;
        this.sql_databases = sql_databases;
        this.neo4j_settings = neo4j_settings;
        this.lock = lock;
    }

    public void run() {
        try {
            String neo4j_db_url = neo4j_settings.get("NEO4J_DB_URL");
            String neo4j_username = neo4j_settings.get("NEO4J_USERNAME");
            String neo4j_password = neo4j_settings.get("NEO4J_PASSWORD");
            org.neo4j.driver.Driver driver = GraphDatabase.driver(neo4j_db_url, AuthTokens.basic(neo4j_username, neo4j_password));
            Session session = driver.session();
            Connection conn = null;
            Statement stmt = null;
            ResultSet resultSet = null;
            List<HashMap> preparedStatementsList = new ArrayList();
            List<Connection> connectionList = new ArrayList();
            for (String db_url : sql_databases.keySet()) {
                String[] db_info = sql_databases.get(db_url);
                String db_driver = db_info[0];
                String db_username = db_info[1];
                String db_password = db_info[2];
                Class.forName(db_driver);
                Connection connection = DriverManager.getConnection(db_url, db_username, db_password);
                connectionList.add(connection);
                PreparedStatement invoice = connection.prepareStatement("INSERT INTO warehouse.invoice (id, customerId, state, duedate, previousinvoice) VALUES (?,?,?,?,?)");
                HashMap<String, PreparedStatement> preparedStatements = new HashMap<String, PreparedStatement>();
                preparedStatements.put("invoice", invoice);
                preparedStatementsList.add(preparedStatements);
            }
            for (int iterator = 0; iterator < sequentialInvoiceCount; iterator++) {
                insertSequentialInvoices(iterator, batchExecuteValue, session, preparedStatementsList);
                invoiceIndex++;
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

    public void insertSequentialInvoices(int iterator, int batchExecuteValue, Session session, List<HashMap> preparedStatementsList) throws SQLException, InterruptedException {
        PreparedStatement invoice;
        String sqlInsert;
        String cypherCreate;
        Random r = new Random();
        r.setSeed(invoiceIndex);
        //-- 0 = incomplete, 1 = complete, 2 = sent, 3 = paid
        int state = 1 + r.nextInt(3);
        GregorianCalendar gregorianCalendar = new GregorianCalendar();
        int year = Calendar.getInstance().get(Calendar.YEAR);
        gregorianCalendar.set(gregorianCalendar.YEAR, year);
        int dayOfYear = 1 + r.nextInt(gregorianCalendar.getActualMaximum(gregorianCalendar.DAY_OF_YEAR));
        gregorianCalendar.set(gregorianCalendar.DAY_OF_YEAR, dayOfYear);
        java.util.Date dueDate = gregorianCalendar.getTime();
        java.sql.Date sqlDueDate = new java.sql.Date(gregorianCalendar.getTime().getTime());
        DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
        String dueDateAsString = dateFormat.format(dueDate);
        for (HashMap<String, PreparedStatement> preparedStatements : preparedStatementsList) {
            invoice = preparedStatements.get("invoice");
            invoice.setInt(1, invoiceIndex);
            invoice.setInt(2, customerIndex);
            invoice.setInt(3, state);
            invoice.setDate(4, sqlDueDate, gregorianCalendar);
            if (invoiceIndex == firstInvoiceIndex) {
                sqlInsert = "INSERT INTO warehouse.invoice (id, customerId, state, duedate, previousinvoice) VALUES (" + invoiceIndex + "," + customerIndex + "," + state + ",STR_TO_DATE('" + dueDateAsString + "','%d-%m-%Y')," + invoiceIndex + ")";
                invoice.setInt(5, invoiceIndex);
            } else {
                sqlInsert = "INSERT INTO warehouse.invoice (id, customerId, state, duedate, previousinvoice) VALUES (" + invoiceIndex + "," + customerIndex + "," + state + ",STR_TO_DATE('" + dueDateAsString + "','%d-%m-%Y')," + (invoiceIndex - 1) + ")";
                invoice.setInt(5, invoiceIndex - 1);
            }
            invoice.addBatch();
        }
        LocalDate localDate = dueDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        int month = localDate.getMonthValue();
        int day = localDate.getDayOfMonth();
        if (invoiceIndex == firstInvoiceIndex) {
            cypherCreate = "CREATE (l:invoice {invoiceId: " + invoiceIndex + ", customerId: " + customerIndex + ", state: " + state + ", duedate: \"date({ year:" + year + ", month:" + month + ", day:" + day + " })\", previousinvoice: " + invoiceIndex + "})";
            writeToNeo4J(session, cypherCreate);
            cypherCreate = "MATCH (a:customer),(l:invoice) WHERE a.customerId = " + customerIndex + " AND l.invoiceId = " + invoiceIndex + " CREATE (a)-[m:PAYS]->(l)";
            writeToNeo4J(session, cypherCreate);
        } else {
            cypherCreate = "CREATE (l:invoice {invoiceId: " + invoiceIndex + ", customerId: " + customerIndex + ", state: " + state + ", duedate: \"date({ year:" + year + ", month:" + month + ", day:" + day + " })\", previousinvoice: " + (invoiceIndex - 1) + "})";
            writeToNeo4J(session, cypherCreate);
            cypherCreate = "MATCH (a:customer),(l:invoice) WHERE a.customerId = " + customerIndex + " AND l.invoiceId = " + invoiceIndex + " CREATE (a)-[m:PAYS]->(l)";
            writeToNeo4J(session, cypherCreate);
            cypherCreate = "MATCH (a:invoice),(b:invoice) WHERE a.invoiceId = " + (invoiceIndex - 1) + " AND b.invoiceId = " + invoiceIndex + " CREATE (a)-[m:PREVIOUS_INVOICE]->(b)";
            writeToNeo4J(session, cypherCreate);
        }
        r.setSeed(invoiceIndex);
        int discountPercent = 1 + r.nextInt(101);
        double discount = (0.01 * discountPercent);
        if (iterator % batchExecuteValue == 0 || iterator == (sequentialInvoiceCount - 1)) {
            for (HashMap<String, PreparedStatement> preparedStatements : preparedStatementsList) {
                invoice = preparedStatements.get("invoice");
                invoice.executeBatch();
            }
        }
    }
}