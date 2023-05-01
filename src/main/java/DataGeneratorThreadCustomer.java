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

public class DataGeneratorThreadCustomer extends Thread {

    private HashMap<String, String[]> sql_databases;
    private HashMap<String, String> neo4j_settings;

    private int iterationCount = 0;
    private int batchExecuteValue = 0;
    private int invoiceFactor = 0;
    private int targetFactor = 0;
    private int workFactor = 0;
    private int sequentialInvoices = 0;
    private int workCount = 0;

    private int threadIndex = 0;
    private int customerIndex = 0;
    private int invoiceIndex = 0;
    private int targetIndex = 0;

    private int firstnameindex = 0;
    private int surnameindex = 0;
    private int addressindex = 0;

    private List<String> firstnames;
    private List<String> surnames;
    private List<HashMap<String, String>> addresses;

    public DataGeneratorThreadCustomer(int threadindex, int iterationCount, int batchExecuteValue, HashMap<String, String[]> sql_databases, HashMap<String, String> neo4j_settings, ReentrantLock lock, int invoiceFactor, int targetFactor, int workFactor, int sequentialInvoices, List<String> firstnames, List<String> surnames, List<HashMap<String, String>> addresses, int customerIndex, int invoiceIndex, int targetIndex, int workCount) {
        this.threadIndex = threadindex;
        this.iterationCount = iterationCount;
        this.batchExecuteValue = batchExecuteValue;
        this.invoiceFactor = invoiceFactor;
        this.targetFactor = targetFactor;
        this.workFactor = workFactor;
        this.sequentialInvoices = sequentialInvoices;
        this.firstnames = firstnames;
        this.surnames = surnames;
        this.addresses = addresses;
        this.customerIndex = customerIndex;
        this.invoiceIndex = invoiceIndex;
        this.targetIndex = targetIndex;
        this.workCount = workCount;
        this.sql_databases = sql_databases;
        this.neo4j_settings = neo4j_settings;
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
                PreparedStatement customer = connection.prepareStatement("INSERT INTO warehouse.customer (id, name, address) VALUES (?,?,?)");
                PreparedStatement invoice = connection.prepareStatement("INSERT INTO warehouse.invoice (id, customerId, state, duedate, previousinvoice) VALUES (?,?,?,?,?)");
                PreparedStatement workInvoice = connection.prepareStatement("INSERT INTO warehouse.workinvoice (workId, invoiceId) VALUES (?,?)");
                PreparedStatement target = connection.prepareStatement("INSERT INTO warehouse.target (id, name, address, customerid) VALUES (?,?,?,?)");
                PreparedStatement workTarget = connection.prepareStatement("INSERT INTO warehouse.worktarget (workId, targetId) VALUES (?,?)");
                HashMap<String, PreparedStatement> preparedStatements = new HashMap<String, PreparedStatement>();
                preparedStatements.put("customer", customer);
                preparedStatements.put("invoice", invoice);
                preparedStatements.put("target", target);
                preparedStatements.put("workinvoice", workInvoice);
                preparedStatements.put("worktarget", workTarget);
                preparedStatementsList.add(preparedStatements);
            }
            for (int iterator = 0; iterator < iterationCount; iterator++) {
                insertCustomer(iterator, batchExecuteValue, session, preparedStatementsList);
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

    public void setIndexes(int index) {
        Random r = new Random(index);
        firstnameindex = r.nextInt(firstnames.size());
        r = new Random(index);
        surnameindex = r.nextInt(surnames.size());
        r = new Random(index);
        addressindex = r.nextInt(addresses.size());
    }

    public void writeToNeo4J(Session session, String cypherQuery) throws SQLException {
        session.writeTransaction(tx -> tx.run(cypherQuery));
    }

    public List<Integer> getWorkIndexes(int index) {
        Random r = new Random();
        List<Integer> allworkIndexes = new ArrayList<Integer>();
        for (int i = 0; i < workCount; i++) {
            allworkIndexes.add(i);
        }
        Collections.shuffle(allworkIndexes, new Random(index));
        List<Integer> selectedWorkIndexes = allworkIndexes.subList(0, workFactor);
        return selectedWorkIndexes;
    }

    public void insertCustomer(int iterator, int batchExecuteValue, Session session, List<HashMap> preparedStatementsList) throws SQLException, InterruptedException {
        PreparedStatement customer;
        PreparedStatement invoice;
        PreparedStatement target;
        PreparedStatement workTarget;
        PreparedStatement workInvoice;
        int i = 0;
        int j = 0;
        System.out.println("Thread: " + threadIndex + " customerIndex: " + customerIndex);
        setIndexes(customerIndex);
        String name = firstnames.get(firstnameindex) + " " + surnames.get(surnameindex);
        String streetAddress = addresses.get(addressindex).get("street") + " " + addresses.get(addressindex).get("city") + " " + addresses.get(addressindex).get("district") + " " + addresses.get(addressindex).get("region") + " " + addresses.get(addressindex).get("postcode");
        String sqlInsert = "INSERT INTO warehouse.customer (id, name, address) VALUES (" + customerIndex + ",\"" + name + "\",\"" + streetAddress + "\")";
        for (HashMap<String, PreparedStatement> preparedStatements : preparedStatementsList) {
            customer = preparedStatements.get("customer");
            customer.setInt(1, customerIndex);
            customer.setString(2, name);
            customer.setString(3, streetAddress);
            customer.addBatch();
        }
        String cypherCreate = "CREATE (a:customer {customerId: " + customerIndex + ", name:\"" + name + "\",address:\"" + streetAddress + "\"})";
        writeToNeo4J(session, cypherCreate);
        int invoiceIndexOriginal = invoiceIndex;
        int firstInvoice = invoiceIndex;
        Random r = new Random(invoiceIndex);
        j = 0;
        while (j < invoiceFactor) {
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
                if (j < sequentialInvoices) {
                    invoice.setInt(1, invoiceIndex);
                    invoice.setInt(2, customerIndex);
                    invoice.setInt(3, state);
                    invoice.setDate(4, sqlDueDate, gregorianCalendar);
                    if (invoiceIndex == firstInvoice) {
                        sqlInsert = "INSERT INTO warehouse.invoice (id, customerId, state, duedate, previousinvoice) VALUES (" + invoiceIndex + "," + customerIndex + "," + state + ",STR_TO_DATE('" + dueDateAsString + "','%d-%m-%Y')," + invoiceIndex + ")";
                        invoice.setInt(5, invoiceIndex);
                    } else {
                        sqlInsert = "INSERT INTO warehouse.invoice (id, customerId, state, duedate, previousinvoice) VALUES (" + invoiceIndex + "," + customerIndex + "," + state + ",STR_TO_DATE('" + dueDateAsString + "','%d-%m-%Y')," + (invoiceIndex - 1) + ")";
                        invoice.setInt(5, invoiceIndex - 1);
                    }
                    invoice.addBatch();
                } else {
                    sqlInsert = "INSERT INTO warehouse.invoice (id, customerId, state, duedate, previousinvoice) VALUES (" + invoiceIndex + "," + customerIndex + "," + state + ",STR_TO_DATE('" + dueDateAsString + "','%d-%m-%Y')," + invoiceIndex + ")";
                    invoice.setInt(1, invoiceIndex);
                    invoice.setInt(2, customerIndex);
                    invoice.setInt(3, state);
                    invoice.setDate(4, sqlDueDate, gregorianCalendar);
                    invoice.setInt(5, invoiceIndex);
                    invoice.addBatch();
                }
            }
            LocalDate localDate = dueDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            int month = localDate.getMonthValue();
            int day = localDate.getDayOfMonth();
            if (j < sequentialInvoices) {
                if (invoiceIndex == firstInvoice) {
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
            } else {
                cypherCreate = "CREATE (l:invoice {invoiceId: " + invoiceIndex + ", customerId: " + customerIndex + ", state: " + state + ", duedate: \"date({ year:" + year + ", month:" + month + ", day:" + day + " })\", previousinvoice: " + invoiceIndex + "})";
                writeToNeo4J(session, cypherCreate);
                cypherCreate = "MATCH (a:customer),(l:invoice) WHERE a.customerId = " + customerIndex + " AND l.invoiceId = " + invoiceIndex + " CREATE (a)-[m:PAYS]->(l)";
                writeToNeo4J(session, cypherCreate);
            }
            invoiceIndex++;
            j++;
        }
        int targetIndexOriginal = targetIndex;
        j = 0;
        while (j < targetFactor) {
            setIndexes(targetIndex);
            name = firstnames.get(firstnameindex) + " " + surnames.get(surnameindex);
            streetAddress = addresses.get(addressindex).get("street") + " " + addresses.get(addressindex).get("city") + " " + addresses.get(addressindex).get("district") + " " + addresses.get(addressindex).get("region") + " " + addresses.get(addressindex).get("postcode");
            sqlInsert = "INSERT INTO warehouse.target (id, name, address, customerid) VALUES (" + targetIndex + ",\"" + name + "\",\"" + streetAddress + "\"," + customerIndex + ")";
            for (HashMap<String, PreparedStatement> preparedStatements : preparedStatementsList) {
                target = preparedStatements.get("target");
                target.setInt(1, targetIndex);
                target.setString(2, name);
                target.setString(3, streetAddress);
                target.setInt(4, customerIndex);
                target.addBatch();
            }
            cypherCreate = "CREATE (t:target {targetId: " + targetIndex + ", name: \"" + name + "\", address: \"" + streetAddress + "\", customerid: " + customerIndex + " })";
            writeToNeo4J(session, cypherCreate);
            cypherCreate = "MATCH (c:customer),(t:target) WHERE c.customerId = " + customerIndex + " AND t.targetId = " + targetIndex + " CREATE (c)-[ct:CUSTOMER_TARGET]->(t)";
            writeToNeo4J(session, cypherCreate);
            targetIndex++;
            j++;
        }
        List<Integer> workIndexes;
        int workIndex = 0;
        targetIndex = targetIndexOriginal;
        i = 0;
        while (i < targetFactor) {
            workIndexes = getWorkIndexes(targetIndex);
            j = 0;
            while (j < workIndexes.size()) {
                workIndex = workIndexes.get(j);
                sqlInsert = "INSERT INTO warehouse.worktarget (workId, targetId) VALUES (" + workIndex + "," + targetIndex + ")";
                for (HashMap<String, PreparedStatement> preparedStatements : preparedStatementsList) {
                    workTarget = preparedStatements.get("worktarget");
                    workTarget.setInt(1, workIndex);
                    workTarget.setInt(2, targetIndex);
                    workTarget.addBatch();
                }
                cypherCreate = "MATCH (w:work),(t:target) WHERE w.workId = " + workIndex + "  AND t.targetId = " + targetIndex + " CREATE (w)-[wt:WORK_TARGET]->(t)";
                //lock.lock();
                writeToNeo4J(session, cypherCreate);
                //Thread.sleep(500);
                //lock.unlock();
                cypherCreate = "MATCH (t:target),(w:work) WHERE t.targetId = " + targetIndex + " AND w.workId = " + workIndex + " CREATE (t)-[wt:WORK_TARGET]->(w)";
                writeToNeo4J(session, cypherCreate);
                j++;
            }
            targetIndex++;
            i++;
        }
        invoiceIndex = invoiceIndexOriginal;
        i = 0;
        while (i < invoiceFactor) {
            workIndexes = getWorkIndexes(invoiceIndex);
            //System.out.println("workIndexes for invoice: " + invoiceIndex);
            //System.out.println(workIndexes.toString());
            j = 0;
            while (j < workIndexes.size()) {
                workIndex = workIndexes.get(j);
                r.setSeed(invoiceIndex);
                sqlInsert = "INSERT INTO warehouse.workinvoice (workId, invoiceId) VALUES (" + workIndex + "," + invoiceIndex + ")";
                for (HashMap<String, PreparedStatement> preparedStatements : preparedStatementsList) {
                    workInvoice = preparedStatements.get("workinvoice");
                    workInvoice.setInt(1, workIndex);
                    workInvoice.setInt(2, invoiceIndex);
                    workInvoice.addBatch();
                }
                cypherCreate = "MATCH (w:work),(i:invoice) WHERE w.workId = " + workIndex + " AND i.invoiceId = " + invoiceIndex + " CREATE (w)-[wi:WORK_INVOICE]->(i)";
                writeToNeo4J(session, cypherCreate);
                cypherCreate = "MATCH (i:invoice),(w:work) WHERE i.invoiceId = " + invoiceIndex + " AND w.workId = " + workIndex + " CREATE (i)-[wi:WORK_INVOICE]->(w)";
                writeToNeo4J(session, cypherCreate);
                j++;
            }
            invoiceIndex++;
            i++;
        }
        workIndex++;
        customerIndex++;
        if (iterator % batchExecuteValue == 0 || iterator == (iterationCount - 1)) {
            for (HashMap<String, PreparedStatement> preparedStatements : preparedStatementsList) {
                customer = preparedStatements.get("customer");
                invoice = preparedStatements.get("invoice");
                target = preparedStatements.get("target");
                workTarget = preparedStatements.get("worktarget");
                workInvoice = preparedStatements.get("workinvoice");
                customer.executeBatch();
                invoice.executeBatch();
                target.executeBatch();
                workTarget.executeBatch();
                workInvoice.executeBatch();
            }
        }
    }
}