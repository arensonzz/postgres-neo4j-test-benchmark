import org.antlr.v4.runtime.atn.SemanticContext;

import java.util.HashMap;

public class Main {
    private static boolean IS_DEBUG_MODE = false;
    private static int QUERY_TEST_ITERATION_COUNT = 12;
    
    public static void main(String[] args) {
        //
        // Parse CLI parameters
        //
        if (args.length >= 1 && args[0].equalsIgnoreCase("debug")) {
            IS_DEBUG_MODE = true;
            if (args.length >= 2) {
                try {
                    QUERY_TEST_ITERATION_COUNT = Integer.parseInt(args[1]);
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
            }
        }
        
        //
        // SQL DBMS Settings
        //
        HashMap<String, String[]> sql_databases = new HashMap<>(); // Map of available SQL DBMSs and their settings
        String[] db_settings = new String[3]; // Array to temporarily store SQL DBMS settings

        String mysql_db_url = "jdbc:postgresql://" + System.getenv("POSTGRES_HOST") + ":5432/"; // DB Connection URL for PostgreSQL
        String db_driver = "org.postgresql.Driver";
        String db_username = "postgres";
        String db_password = System.getenv("POSTGRES_PASSWORD");

        db_settings[0] = db_driver;
        db_settings[1] = db_username;
        db_settings[2] = db_password;

        sql_databases.put(mysql_db_url, db_settings); // Add PostgreSQL as SQL DBMS

        //
        // Neo4j DBMS Settings
        //
        HashMap<String, String> neo4j_settings = new HashMap<>(); // Map of Neo4j settings

        String neo4j_db_url = "bolt://" + System.getenv("NEO4J_HOST") + ":7687"; // DB connection URL for Neo4j
        String neo4j_auth = System.getenv("NEO4J_AUTH"); // Get Neo4j authentication string from the system
        String neo4j_password = neo4j_auth.substring(neo4j_auth.lastIndexOf("/") + 1);
        String neo4j_username = neo4j_auth.substring(0, neo4j_auth.lastIndexOf("/"));

        neo4j_settings.put("NEO4J_DB_URL", neo4j_db_url);
        neo4j_settings.put("NEO4J_USERNAME", neo4j_username);
        neo4j_settings.put("NEO4J_PASSWORD", neo4j_password);

        // DataGenerator is used both for DDL and DML operations. It first creates tables.
        // Then loads sample data from csv files, and by using that data generates necessary SQL tables
        // and Neo4j nodes for the benchmark.
        DataGenerator dataGenerator = new DataGenerator(sql_databases, neo4j_settings, mysql_db_url);

        // Create tables for "warehouse" and "testdata" databases
        dataGenerator.createTables();
        dataGenerator.createSampleTables(mysql_db_url);
        // Insert sample data into "testdata" database.
        // This will be used for generating "warehouse" table records.
        dataGenerator.loadSampleData(10, mysql_db_url);

        // Generate benchmark records for "warehouse" database
        
        if (IS_DEBUG_MODE) {
            dataGenerator.insertItemsAndWorkTypes(2, 10, 10, 100);
            dataGenerator.insertWorkData(2, 10, 10, 10, 10);
            dataGenerator.insertCustomerData(2, 10, 10, 10, 0, 10, 10);
        } else {
            dataGenerator.insertItemsAndWorkTypes(10, 10, 10000, 10000);
            dataGenerator.insertWorkData(10, 1000, 10, 10, 10);
            dataGenerator.insertCustomerData(10, 1000, 10, 10, 0, 10, 10);
        }

        // QueryTester is used for 
        QueryTester queryTester = new QueryTester(sql_databases, neo4j_settings);

        System.out.println("NO INDEXES");

        queryTester.executeQueryTestsSQL(QUERY_TEST_ITERATION_COUNT, true);
        queryTester.executeQueryTestsCypher(QUERY_TEST_ITERATION_COUNT, true);

        System.out.println();
        System.out.println("CREATING INDEXES");
        System.out.println();

        dataGenerator.createIndexes();

        queryTester.executeQueryTestsSQL(QUERY_TEST_ITERATION_COUNT, true);
        queryTester.executeQueryTestsCypher(QUERY_TEST_ITERATION_COUNT, true);


        System.out.println();
        System.out.println("DELETING INDEXES");
        System.out.println();

        dataGenerator.deleteIndexes();

        queryTester.executeComplexQueryTestSQL(QUERY_TEST_ITERATION_COUNT, true);
        queryTester.executeComplexQueryTestCypher(QUERY_TEST_ITERATION_COUNT, true);

        System.out.println();
        System.out.println("CREATING INDEXES");
        System.out.println();

        dataGenerator.createIndexes();

        queryTester.executeComplexQueryTestSQL(QUERY_TEST_ITERATION_COUNT, true);
        queryTester.executeComplexQueryTestCypher(QUERY_TEST_ITERATION_COUNT, true);

        System.out.println();
        System.out.println("DELETING INDEXES");
        System.out.println();

        dataGenerator.deleteIndexes();

        //System.out.println();
        //System.out.println("REMOVING MySQL");
        //System.out.println();
        //sql_databases.remove(mysql_db_url);


        queryTester.executeQueryWithDefinedKeySQL(QUERY_TEST_ITERATION_COUNT, true);
        queryTester.executeQueryWithDefinedKeyCypher(QUERY_TEST_ITERATION_COUNT, true);

        System.out.println();
        System.out.println("CREATING INDEXES");
        System.out.println();

        dataGenerator.createIndexes();

        queryTester.executeQueryWithDefinedKeySQL(QUERY_TEST_ITERATION_COUNT, true);
        queryTester.executeQueryWithDefinedKeyCypher(QUERY_TEST_ITERATION_COUNT, true);

        System.out.println();
        System.out.println("DELETING INDEXES");
        System.out.println();

        dataGenerator.deleteIndexes();
        
        HashMap<String, Integer> customerInvoice = dataGenerator.insertSequentialInvoices(1, 10, (IS_DEBUG_MODE) ? 10 : 100);

        int invoiceIndex = customerInvoice.get("invoiceIndex");
        int customerIndex = customerInvoice.get("customerIndex");

        queryTester.executeRecursiveQueryTestSQL(QUERY_TEST_ITERATION_COUNT, true, invoiceIndex);
        queryTester.executeRecursiveQueryTestCypher(QUERY_TEST_ITERATION_COUNT, true, invoiceIndex);

        System.out.println("customerIndex " + customerIndex);
        dataGenerator.cleanSequentialInvoices(customerIndex);

        customerInvoice = dataGenerator.insertSequentialInvoices(1, 10, (IS_DEBUG_MODE) ? 100 : 1000);

        invoiceIndex = customerInvoice.get("invoiceIndex");

        queryTester.executeRecursiveQueryTestSQL(QUERY_TEST_ITERATION_COUNT, true, invoiceIndex);
        queryTester.executeRecursiveQueryTestCypher(QUERY_TEST_ITERATION_COUNT, true, invoiceIndex);

        dataGenerator.cleanSequentialInvoices(customerIndex);

        System.out.println();
        System.out.println("CREATING INDEXES");
        System.out.println();

        dataGenerator.createIndexes();

        customerInvoice = dataGenerator.insertSequentialInvoices(1, 10, (IS_DEBUG_MODE) ? 10 : 100);

        invoiceIndex = customerInvoice.get("invoiceIndex");
        customerIndex = customerInvoice.get("customerIndex");

        queryTester.executeRecursiveQueryTestSQL(QUERY_TEST_ITERATION_COUNT, true, invoiceIndex);
        queryTester.executeRecursiveQueryTestCypher(QUERY_TEST_ITERATION_COUNT, true, invoiceIndex);

        System.out.println("customerIndex " + customerIndex);
        dataGenerator.cleanSequentialInvoices(customerIndex);

        customerInvoice = dataGenerator.insertSequentialInvoices(1, 10, (IS_DEBUG_MODE) ? 10 : 1000);

        invoiceIndex = customerInvoice.get("invoiceIndex");

        queryTester.executeRecursiveQueryTestSQL(QUERY_TEST_ITERATION_COUNT, true, invoiceIndex);
        queryTester.executeRecursiveQueryTestCypher(QUERY_TEST_ITERATION_COUNT, true, invoiceIndex);

        dataGenerator.cleanSequentialInvoices(customerIndex);
    }
}
