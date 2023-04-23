import java.util.HashMap;

public class Main
{
    public static void main(String[] args)
    {
        HashMap<String, String[]> sql_databases = new HashMap<>();

        String[] db_settings = new String[3];

        String mysql_db_url = "jdbc:postgresql://localhost:5432/";
        String db_driver = "org.postgresql.Driver";
        String db_username = "postgres";
        String db_password = System.getenv("POSTGRES_PASSWORD");

        db_settings[0] = db_driver;
        db_settings[1] = db_username;
        db_settings[2] = db_password;

        sql_databases.put(mysql_db_url, db_settings);

        HashMap<String, String> neo4j_settings = new HashMap<>();

        String neo4J_db_url = "bolt://localhost:7687";
        String neo4J_auth = System.getenv("NEO4J_AUTH");
        String neo4J_password = neo4J_auth.substring(neo4J_auth.lastIndexOf("/") + 1);
        String neo4J_username = neo4J_auth.substring(0, neo4J_auth.lastIndexOf("/"));

        System.out.println("neo4J_auth: " + neo4J_auth);
        System.out.println("neo4j_password: " + neo4J_password);
        System.out.println("neo4j_username: " + neo4J_username);

        neo4j_settings.put("NEO4J_DB_URL", neo4J_db_url);
        neo4j_settings.put("NEO4J_USERNAME", neo4J_username);
        neo4j_settings.put("NEO4J_PASSWORD", neo4J_password);

        DataGenerator dataGenerator = new DataGenerator(sql_databases, neo4j_settings, mysql_db_url);

        dataGenerator.createTables();

        dataGenerator.createSampleTables(mysql_db_url);

        dataGenerator.loadSampleData(10, mysql_db_url);

        dataGenerator.insertItemsAndWorkTypes(10, 10, 10000, 10000);
        dataGenerator.insertWorkData(10,1000,10,10,10);
        dataGenerator.insertCustomerData(10,1000,10,10,0,10,10);

        QueryTester queryTester = new QueryTester(sql_databases, neo4j_settings);

        System.out.println("NO INDEXES");

        queryTester.executeQueryTestsSQL(12, true);
        queryTester.executeQueryTestsCypher(12, true);

        System.out.println();
        System.out.println("CREATING INDEXES");
        System.out.println();

        dataGenerator.createIndexes();

        queryTester.executeQueryTestsSQL(12, true);
        queryTester.executeQueryTestsCypher(12, true);


        System.out.println();
        System.out.println("DELETING INDEXES");
        System.out.println();

        dataGenerator.deleteIndexes();

        queryTester.executeComplexQueryTestSQL(12, true);
        queryTester.executeComplexQueryTestCypher(12, true);

        System.out.println();
        System.out.println("CREATING INDEXES");
        System.out.println();

        dataGenerator.createIndexes();

        queryTester.executeComplexQueryTestSQL(12, true);
        queryTester.executeComplexQueryTestCypher(12, true);

        System.out.println();
        System.out.println("DELETING INDEXES");
        System.out.println();

        dataGenerator.deleteIndexes();

        System.out.println();
        System.out.println("REMOVING MySQL");
        System.out.println();
        sql_databases.remove(mysql_db_url);


        queryTester.executeQueryWithDefinedKeySQL(12, true);
        queryTester.executeQueryWithDefinedKeyCypher(12, true);

        System.out.println();
        System.out.println("CREATING INDEXES");
        System.out.println();

        dataGenerator.createIndexes();

        queryTester.executeQueryWithDefinedKeySQL(12, true);
        queryTester.executeQueryWithDefinedKeyCypher(12, true);

        System.out.println();
        System.out.println("DELETING INDEXES");
        System.out.println();

        dataGenerator.deleteIndexes();

        HashMap<String, Integer> customerInvoice =  dataGenerator.insertSequentialInvoices(1,10,100);

        int invoiceIndex = customerInvoice.get("invoiceIndex");
        int customerIndex = customerInvoice.get("customerIndex");

        queryTester.executeRecursiveQueryTestSQL(12, true, invoiceIndex);
        queryTester.executeRecursiveQueryTestCypher(12, true, invoiceIndex);

        System.out.println("customerIndex " + customerIndex);
        dataGenerator.cleanSequentialInvoices(customerIndex);

        customerInvoice =  dataGenerator.insertSequentialInvoices(1,10,1000);

        invoiceIndex = customerInvoice.get("invoiceIndex");

        queryTester.executeRecursiveQueryTestSQL(12, true, invoiceIndex);
        queryTester.executeRecursiveQueryTestCypher(12, true, invoiceIndex);

        dataGenerator.cleanSequentialInvoices(customerIndex);

        System.out.println();
        System.out.println("CREATING INDEXES");
        System.out.println();

        dataGenerator.createIndexes();

        customerInvoice =  dataGenerator.insertSequentialInvoices(1,10,100);

        invoiceIndex = customerInvoice.get("invoiceIndex");
        customerIndex = customerInvoice.get("customerIndex");

        queryTester.executeRecursiveQueryTestSQL(12, true, invoiceIndex);
        queryTester.executeRecursiveQueryTestCypher(12, true, invoiceIndex);

        System.out.println("customerIndex " + customerIndex);
        dataGenerator.cleanSequentialInvoices(customerIndex);

        customerInvoice =  dataGenerator.insertSequentialInvoices(1,10,1000);

        invoiceIndex = customerInvoice.get("invoiceIndex");

        queryTester.executeRecursiveQueryTestSQL(12, true, invoiceIndex);
        queryTester.executeRecursiveQueryTestCypher(12, true, invoiceIndex);
        
        dataGenerator.cleanSequentialInvoices(customerIndex);

    }
}
