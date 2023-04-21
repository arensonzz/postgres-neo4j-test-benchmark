
import org.neo4j.driver.*;
import org.neo4j.driver.Record;

import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class QueryTester {

    private HashMap<String, String[]> sql_databases;

    private HashMap<String, String> neo4j_settings;

    private HashMap<String, ArrayList<Long>> resultLists;

    private List<Long> results;

    public QueryTester(HashMap<String, String[]> sql_databases, HashMap<String, String> neo4j_settings) {
        this.sql_databases = sql_databases;
        this.neo4j_settings = neo4j_settings;
    }

    public HashMap<String, ArrayList<Long>> measureQueryTimeSQL(String sqlQuery, int iterations) {

        HashMap<String, ArrayList<Long>> resultLists = new HashMap<String, ArrayList<Long>>();

        ArrayList<Long> results = null;

        Connection connection = null;
        Statement stmt = null;

        System.out.println("Executing SQL Query: " + sqlQuery + " in " + sql_databases.size() + " databases with " + iterations + " iterations.");

        try {

            for (String db_url : sql_databases.keySet()) {

                String[] db_info = sql_databases.get(db_url);

                String db_driver = db_info[0];
                String db_username = db_info[1];
                String db_password = db_info[2];

                Class.forName(db_driver);

                connection = DriverManager.getConnection(db_url + "warehouse", db_username, db_password);

                DatabaseMetaData meta = connection.getMetaData();

                String productName = meta.getDatabaseProductName();
                String productVersion = meta.getDatabaseProductVersion();

                stmt = connection.createStatement();

                results = new ArrayList<Long>();

                ResultSet resultSet = null;

                for(int i=0; i<iterations; i++) {

                    System.out.println("Starting iteration: " + i + ".");

                    long startTimeInMilliseconds = System.currentTimeMillis();

                    resultSet = stmt.executeQuery(sqlQuery);

                    long endTimeInMilliseconds = System.currentTimeMillis();
                    long elapsedTimeMilliseconds = endTimeInMilliseconds - startTimeInMilliseconds;

                    results.add(elapsedTimeMilliseconds);

                }

                resultLists.put(productVersion, results);

                if(resultSet != null) {
                    resultSet.last();
                    System.out.println("Query in url " + db_url + " returned " + resultSet.getRow() + " rows.");
                } else {
                    System.out.println("Query in url " + db_url + " returned 0 rows.");                     }
                }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {

            try {
                if (stmt != null) {
                    connection.close();
                }
            } catch (SQLException se) {
            }
            try {
                if (connection != null) {
                    connection.close();
                }
            } catch (SQLException se) {
                se.printStackTrace();
            }
        }

        System.out.println();

        return resultLists;
    }

    public List<Long> measureQueryTimeCypher(String cypherQuery, int iterations) {

        String neo4j_db_url = neo4j_settings.get("NEO4J_DB_URL");
        String neo4j_username = neo4j_settings.get("NEO4J_USERNAME");
        String neo4j_password = neo4j_settings.get("NEO4J_PASSWORD");

        org.neo4j.driver.Driver driver = GraphDatabase.driver(neo4j_db_url, AuthTokens.basic(neo4j_username, neo4j_password));

        Session session = driver.session();

        List<Long> results = new ArrayList<Long>();

        Result result = null;

        System.out.println("Executing Cypher Query: " + cypherQuery + " with " + iterations + " iterations.");

        for(int i=0; i<iterations; i++) {

            System.out.println("Starting iteration: " + i + ".");

            long startTimeInMilliseconds = System.currentTimeMillis();

            result = session.run(cypherQuery);

            long endTimeInMilliseconds = System.currentTimeMillis();
            long elapsedTimeMilliseconds = endTimeInMilliseconds - startTimeInMilliseconds;

            results.add(elapsedTimeMilliseconds);

        }

        if(result != null) {
            List<Record> records = result.list();
            System.out.println("Cypher query returned: " + records.size() + " records.");
        } else {
            System.out.println("Cypher query returned: 0 records.");
        }
        session.close();
        driver.close();

        return results;
    }

    public void showResults(List<Long> results, boolean showAll) {

        if(results.size() == 0) {
            return;
        }

        Collections.sort(results);

        if(showAll) {
            System.out.println("Smallest number in resultset: ");
            System.out.println(results.get(0));
            System.out.println("Biggest number in resultset: ");
            System.out.println(results.get(results.size() - 1));
        }

        if(results.size() > 2) {
            results.remove(0);
            results.remove(results.size() - 1);
        }

        long sum = 0;

        if(showAll) {
            System.out.println();
            System.out.println("Content of the results table:");
        }

        for(int i=0; i<results.size(); i++) {

            if(showAll) {
                System.out.println(results.get(i));
            }
            sum = sum + results.get(i);
        }

        double average = sum / results.size();

        double standardDeviation = calculateStandardDeviation(results);

        if(showAll) {
            System.out.println();
        }

        System.out.println("Average time for query: ");
        System.out.println(average);
        System.out.println();

        System.out.println("Standard deviation of the results array: ");
        System.out.println(standardDeviation);
        System.out.println();



    }

    public static double calculateStandardDeviation(List<Long> results)
    {
        double sum = 0.0, standardDeviation = 0.0;
        int size = results.size();

        for(long result : results) {
            sum += result;
        }

        double mean = sum/size;

        for(double result: results) {
            standardDeviation += Math.pow(result - mean, 2);
        }

        return Math.sqrt(standardDeviation/size);
    }

    public void executeQueryTestsSQL(int iterations, boolean showAll) {

        System.out.println("Short query, work price");

        String workPriceSQL =
            "SELECT work.id AS workId, " +
            "SUM( " +
            "(worktype.price * workhours.hours * workhours.discount) " +
            ") AS price " +
            "FROM work " +
            "INNER JOIN workhours ON work.id = workhours.workId " +
            "INNER JOIN worktype ON worktype.id = workhours.worktypeId " +
            "GROUP BY work.id";

        resultLists = measureQueryTimeSQL(workPriceSQL, iterations);

        for (String databaseVersion : resultLists.keySet()) {

            if(databaseVersion.contains("MariaDB")) {
                System.out.println("Results for MariaDB version " + databaseVersion);
            }
            else {
                System.out.println("Results for MySQL version " + databaseVersion);
            }

            results = resultLists.get(databaseVersion);
            showResults(results, showAll);

        }

        System.out.println("Long query, work price");

        String workPriceWithItemsSQL =
            "SELECT work.id AS workId, " +
            "SUM(" +
            "(worktype.price * workhours.hours * workhours.discount) + " +
            "(item.purchaseprice * useditem.amount * useditem.discount) " +
            ") AS price " +
            "FROM work " +
            "INNER JOIN workhours ON work.id = workhours.workId " +
            "INNER JOIN worktype ON worktype.id = workhours.worktypeId " +
            "INNER JOIN useditem ON work.id = useditem.workId " +
            "INNER JOIN item ON useditem.itemId = item.id " +
            "GROUP BY work.id";



        resultLists = measureQueryTimeSQL(workPriceWithItemsSQL, iterations);

        for (String databaseVersion : resultLists.keySet()) {

            if(databaseVersion.contains("MariaDB")) {
                System.out.println("Results for MariaDB version " + databaseVersion);
            }
            else {
                System.out.println("Results for MySQL version " + databaseVersion);
            }

            results = resultLists.get(databaseVersion);
            showResults(results, showAll);

        }

        System.out.println("Query with defined key, work of invoiceId 0");

        String workOfInvoiceSQL = "SELECT * FROM work INNER JOIN workInvoice ON work.id=workInvoice.workId INNER JOIN invoice ON workInvoice.workId=invoice.id AND invoice.id=0";

        resultLists = measureQueryTimeSQL(workOfInvoiceSQL, iterations);

        for (String databaseVersion : resultLists.keySet()) {

            if(databaseVersion.contains("MariaDB")) {
                System.out.println("Results for MariaDB version " + databaseVersion);
            }
            else {
                System.out.println("Results for MySQL version " + databaseVersion);
            }

            results = resultLists.get(databaseVersion);
            showResults(results, showAll);

        }

    }

    public void executeQueryWithDefinedKeySQL(int iterations, boolean showAll) {

        System.out.println("Query with defined key, invoice prices for customerId 0");

        String invoicePricesForCustomerSQL = "SELECT q1.customerId, q2.invoiceId, SUM(q3.price) AS invoicePrice FROM " +
                "( SELECT customer.id AS customerId, invoice.id AS invoiceId FROM invoice INNER JOIN customer ON invoice.customerId=customer.id ) AS q1 INNER JOIN " +
                "( SELECT workinvoice.invoiceId, workinvoice.workId FROM workinvoice INNER JOIN invoice ON workinvoice.invoiceId = invoice.id ) AS q2 USING (invoiceId) INNER JOIN " +
                "( SELECT workhours.workid AS workId, SUM( (worktype.price * workhours.hours * workhours.discount) + (item.purchaseprice * useditem.amount * useditem.discount) ) AS price FROM workhours INNER JOIN worktype ON workhours.worktypeid = worktype.id INNER JOIN useditem ON workhours.workid = useditem.workid INNER JOIN item ON useditem.itemid = item.id GROUP BY workhours.workid ) " +
                "AS q3 USING (workId) WHERE q1.customerId=0 GROUP BY q2.invoiceId";

        resultLists = measureQueryTimeSQL(invoicePricesForCustomerSQL, iterations);

        for (String databaseVersion : resultLists.keySet()) {

            if(databaseVersion.contains("MariaDB")) {
                System.out.println("Results for MariaDB version " + databaseVersion);
            }
            else {
                System.out.println("Results for MySQL version " + databaseVersion);
            }

            results = resultLists.get(databaseVersion);
            showResults(results, showAll);

        }

    }

    public void executeQueryWithDefinedKeyCypher(int iterations, boolean showAll) {

        System.out.println();

        System.out.println("Query with defined key, invoice prices for customerId 0");

        String invoicePricesForCustomerCypher = "MATCH (c:customer)-[:PAYS]->(inv:invoice) WHERE c.customerId=0 " +
                "WITH c, inv " +
                "OPTIONAL MATCH (inv)-[:WORK_INVOICE]->(w:work) " +
                "WITH c, inv, w " +
                "OPTIONAL MATCH (wt:worktype)-[h:WORKHOURS]->(w:work)-[u:USED_ITEM]->(i:item) " +
                "WITH c, inv, w, SUM((h.hours*h.discount*wt.price)+(u.amount*u.discount*i.purchaseprice)) as workPrice " +
                "RETURN c, inv, SUM(workPrice) as invoicePrice";

        results = measureQueryTimeCypher(invoicePricesForCustomerCypher, iterations);

        showResults(results, showAll);

        System.out.println();


        System.out.println("Query with defined key with CALL, invoice prices for customerId 0");

        String invoicePricesForCustomerCypher3 = "MATCH (inv:invoice) WHERE inv.customerId=0 " +
        "CALL { " +
        "   WITH inv " +
        "   MATCH (c:customer)-[:PAYS]->(inv) " +
        "   RETURN c " +
        "}" +
        "CALL { " +
        "   WITH c, inv " +
        "   MATCH (inv)-[:WORK_INVOICE]->(w:work) " +
        "   RETURN w " +
        "} " +
        "CALL { " +
        "   WITH w " +
        "   MATCH (wt:worktype)-[h:WORKHOURS]->(w)-[u:USED_ITEM]->(i:item) " +
        "   RETURN SUM((h.hours*h.discount*wt.price)+(u.amount*u.discount*i.purchaseprice)) as workPrice " +
        "} " +
        "RETURN c, inv, SUM(workPrice) as invoicePrice";

        results = measureQueryTimeCypher(invoicePricesForCustomerCypher3, iterations);

        showResults(results, showAll);

        /*


         */


    }

    public void executeQueryTestsCypher(int iterations, boolean showAll) {

        System.out.println("Short query1, work price");

        String workPriceCypher = "MATCH (wt:worktype)-[h:WORKHOURS]->(w:work) WITH SUM(h.hours*h.discount*wt.price) as price, w RETURN w.workId as workId, price;";

        results = measureQueryTimeCypher(workPriceCypher, iterations);

        showResults(results, showAll);

        System.out.println();

        System.out.println("Short query2, work price");

        String workPriceCypher2 = "MATCH (w:work) " +
                "CALL { " +
                "    WITH w " +
                "    MATCH (wt:worktype)-[h:WORKHOURS]->(w) " +
                "    RETURN SUM((h.hours*h.discount*wt.price)) as price " +
                "} " +
                "RETURN w.workId as workId, price;";

        results = measureQueryTimeCypher(workPriceCypher2, iterations);

        showResults(results, showAll);

        System.out.println();


        System.out.println("Long query1, work price");

        String workPriceWithItemsCypher = "MATCH (wt:worktype)-[h:WORKHOURS]->(w:work)-[u:USED_ITEM]->(i:item) WITH SUM((h.hours*h.discount*wt.price)+(u.amount*u.discount*i.purchaseprice)) as price, w RETURN w.workId as workId, price";

        results = measureQueryTimeCypher(workPriceWithItemsCypher, iterations);

        showResults(results, showAll);

        System.out.println("Long query2, work price");

        String workPriceWithItemsCypher2 = "MATCH (w:work) " +
                "CALL { " +
                "    WITH w " +
                "    MATCH (wt:worktype)-[h:WORKHOURS]->(w)-[u:USED_ITEM]->(i:item) " +
                "    RETURN SUM((h.hours*h.discount*wt.price)+(u.amount*u.discount*i.purchaseprice)) as price " +
                "} " +
                "RETURN w.workId as workId, price;";

        results = measureQueryTimeCypher(workPriceWithItemsCypher2, iterations);

        showResults(results, showAll);

        System.out.println();

        System.out.println("Query with defined key, work of invoice");

        String workOfInvoiceCypher = "MATCH (i:invoice { invoiceId:0 })-[wi:WORK_INVOICE]->(w:work) RETURN *";

        results = measureQueryTimeCypher(workOfInvoiceCypher, iterations);

        showResults(results, showAll);

    }

    public void executeComplexQueryTestSQL(int iterations, boolean showAll) {

        System.out.println("Complex query, invoice price");

        String invoicePriceSQL =
            "SELECT q1.invoiceId, SUM(q2.price) AS invoicePrice " +
            "FROM ( " +
            "SELECT workinvoice.invoiceId, workinvoice.workId " +
            "FROM workinvoice " +
            "INNER JOIN invoice ON workinvoice.invoiceId = invoice.id " +
            ") AS q1 " +
            "INNER JOIN ( " +
            "SELECT workhours.workid AS workId, " +
            "SUM( " +
            "(worktype.price * workhours.hours * workhours.discount) + " +
            "(item.purchaseprice * useditem.amount * useditem.discount) " +
            ") AS price " +
            "FROM workhours " +
            "INNER JOIN worktype ON workhours.worktypeid = worktype.id " +
            "INNER JOIN useditem ON workhours.workid = useditem.workid " +
            "INNER JOIN item ON useditem.itemid = item.id " +
            "GROUP BY workhours.workid " +
            ") AS q2 USING (workId) " +
            "GROUP BY q1.invoiceId";

        resultLists = measureQueryTimeSQL(invoicePriceSQL, iterations);

        for (String databaseVersion : resultLists.keySet()) {

            if(databaseVersion.contains("MariaDB")) {
                System.out.println("Results for MariaDB version " + databaseVersion);
            }
            else {
                System.out.println("Results for MySQL version " + databaseVersion);
            }

            results = resultLists.get(databaseVersion);
            showResults(results, showAll);

        }

        System.out.println();

    }

    public void executeComplexQueryTestCypher(int iterations, boolean showAll) {

        System.out.println("Complex query, invoice price");

        System.out.println();

        String invoicePriceCypher = "MATCH (inv:invoice)-[:WORK_INVOICE]->(w:work) " +
        "WITH inv, w " +
        "OPTIONAL MATCH (wt:worktype)-[h:WORKHOURS]->(w:work)-[u:USED_ITEM]->(i:item) " +
        "WITH inv, w, SUM((h.hours*h.discount*wt.price)+(u.amount*u.discount*i.purchaseprice)) as workPrice " +
        "RETURN inv, SUM(workPrice) as invoicePrice";

        results = measureQueryTimeCypher(invoicePriceCypher, iterations);

        showResults(results, showAll);

        System.out.println("Complex query with CALL, invoice price");

        System.out.println();

        String invoicePriceCypher3 =
                "MATCH (inv:invoice) " +
                "CALL { " +
                "WITH inv " +
                "MATCH (inv)-[:WORK_INVOICE]->(w:work) " +
                "RETURN w" +
                "} " +
                "CALL { " +
                "WITH w " +
                "MATCH (wt:worktype)-[h:WORKHOURS]->(w)-[u:USED_ITEM]->(i:item) " +
                "RETURN SUM((h.hours*h.discount*wt.price)+(u.amount*u.discount*i.purchaseprice)) as workPrice " +
                "} " +
                "RETURN inv, SUM(workPrice) as invoicePrice";

        results = measureQueryTimeCypher(invoicePriceCypher3, iterations);

        showResults(results, showAll);

    }



    public void executeCyclicQueryTestSQL(int iterations, boolean showAll, int invoiceId) {

        System.out.println("Executing recursive query test");

        System.out.println("Cyclic query SQL, invoices related to invoice id " + invoiceId);

        String previousInvoicesSQL = "SELECT  id,customerid,state,duedate,previousinvoice " +
                "FROM (SELECT * FROM invoice " +
                "ORDER BY previousinvoice, id) invoices_sorted, " +
                "(SELECT @pv := '" + invoiceId + "') initialisation " +
                "WHERE find_in_set(previousinvoice, @pv) " +
                "AND length(@pv := concat(@pv, ',', id))";

        resultLists = measureQueryTimeSQL(previousInvoicesSQL, iterations);

        for (String databaseVersion : resultLists.keySet()) {

            if(databaseVersion.contains("MariaDB")) {
                System.out.println("Results for MariaDB version " + databaseVersion);
            }
            else {
                System.out.println("Results for MySQL version " + databaseVersion);
            }

            results = resultLists.get(databaseVersion);
            showResults(results, showAll);

        }

    }

    public void executeRecursiveQueryTestCypher(int iterations, boolean showAll, int invoiceId) {

        System.out.println("Executing recursive query test");

        System.out.println("Recursive query Cypher, invoices related to invoice id " + invoiceId);

        String previousInvoicesCypher = "MATCH (i:invoice { invoiceId:" + invoiceId + " })-[p:PREVIOUS_INVOICE *0..]->(j:invoice) RETURN *";

        results = measureQueryTimeCypher(previousInvoicesCypher, iterations);

        showResults(results, showAll);

        System.out.println();

        System.out.println("Recursive query Cypher optimized, invoices related to invoice id " + invoiceId);

        String previousInvoicesCypherOptimized = "MATCH inv=(i:invoice { invoiceId:" + invoiceId + "})-[p:PREVIOUS_INVOICE *0..]->(j:invoice) WHERE NOT (j)-[:PREVIOUS_INVOICE]->() RETURN nodes(inv)";

        results = measureQueryTimeCypher(previousInvoicesCypherOptimized, iterations);

        showResults(results, showAll);


    }

    public void executeRecursiveQueryTestSQL(int iterations, boolean showAll, int invoiceId) {

        System.out.println("Executing recursive query test for optimized queries");

        System.out.println("Recursive query SQL with Common Table Expressions, invoices related to invoice id " + invoiceId);

        String previousInvoicesCTESQL = "WITH RECURSIVE previous_invoices AS (" +
                "SELECT id, customerId, state, duedate, previousinvoice " +
                "FROM invoice " +
                "WHERE id=" + invoiceId + " " +
                "UNION ALL " +
                "SELECT i.id, i.customerId, i.state, i.duedate, i.previousinvoice " +
                "FROM invoice AS i INNER JOIN previous_invoices AS j " +
                "ON i.previousinvoice = j.id " +
                "WHERE i.previousinvoice <> i.id" +
                ") " +
                "SELECT * FROM previous_invoices";

        HashMap<String, String[]> tempSql_databases = (HashMap<String, String[]>) this.sql_databases.clone();

        this.sql_databases.remove("jdbc:mysql://127.0.0.1:3307/");

        resultLists = measureQueryTimeSQL(previousInvoicesCTESQL, iterations);

        for (String databaseVersion : resultLists.keySet()) {

            if(databaseVersion.contains("MariaDB")) {
                System.out.println("Results for MariaDB version " + databaseVersion);
            }

            results = resultLists.get(databaseVersion);
            showResults(results, showAll);

        }

        this.sql_databases = (HashMap<String, String[]>) tempSql_databases.clone();

    }

}