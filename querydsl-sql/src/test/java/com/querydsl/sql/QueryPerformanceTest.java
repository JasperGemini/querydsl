package com.querydsl.sql;

import static org.junit.Assert.assertNotNull;

import java.sql.*;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import com.mysema.commons.lang.CloseableIterator;
import com.querydsl.core.DefaultQueryMetadata;
import com.querydsl.core.JoinType;
import com.querydsl.core.QueryMetadata;
import com.querydsl.core.testutil.Benchmark;
import com.querydsl.core.testutil.H2;
import com.querydsl.core.testutil.Performance;
import com.querydsl.core.testutil.Runner;

@Category({H2.class, Performance.class})
public class QueryPerformanceTest {

    private static final String QUERY = "select COMPANIES.NAME\n" +
            "from COMPANIES COMPANIES\n" +
            "where COMPANIES.ID = ?";

    private static final SQLTemplates templates = new H2Templates();

    private static final Configuration conf = new Configuration(templates);

    private final Connection conn = Connections.getConnection();

    @BeforeClass
    public static void setUpClass() throws SQLException, ClassNotFoundException {
        Connections.initH2();
        Connection conn = Connections.getConnection();
        Statement stmt = conn.createStatement();
        stmt.execute("create or replace table companies (id identity, name varchar(30) unique not null);");

        PreparedStatement pstmt = conn.prepareStatement("insert into companies (name) values (?)");
        final int iterations = 1000000;
        for (int i = 0; i < iterations; i++) {
            pstmt.setString(1, String.valueOf(i));
            pstmt.execute();
            pstmt.clearParameters();
        }
        pstmt.close();
        stmt.close();

        conn.setAutoCommit(false);
    }

    @AfterClass
    public static void tearDownClass() throws SQLException {
        Connection conn = Connections.getConnection();
        Statement stmt = conn.createStatement();
        stmt.execute("drop table companies");
        stmt.close();
        Connections.close();
    }


    @Test
    public void jDBC() throws Exception {
        Runner.run("jdbc by id", new Benchmark() {
            @Override
            public void run(int times) throws Exception {
                for (int i = 0; i < times; i++) {
                    PreparedStatement stmt = conn.prepareStatement(QUERY);
                    try {
                        stmt.setLong(1, i);
                        ResultSet rs = stmt.executeQuery();
                        try {
                            while (rs.next()) {
                                rs.getString(1);
                            }
                        } finally {
                            rs.close();
                        }

                    } finally {
                        stmt.close();
                    }
                }
            }
        });
    }

    @Test
    public void jDBC2() throws Exception {
        Runner.run("jdbc by name", new Benchmark() {
            @Override
            public void run(int times) throws Exception {
                for (int i = 0; i < times; i++) {
                    PreparedStatement stmt = conn.prepareStatement(QUERY);
                    try {
                        stmt.setString(1, String.valueOf(i));
                        ResultSet rs = stmt.executeQuery();
                        try {
                            while (rs.next()) {
                                rs.getString(1);
                            }
                        } finally {
                            rs.close();
                        }

                    } finally {
                        stmt.close();
                    }
                }
            }
        });
    }

    @Test
    public void querydsl1() throws Exception {
        Runner.run("qdsl by id", new Benchmark() {
            @Override
            public void run(int times) throws Exception {
                for (int i = 0; i < times; i++) {
                    QCompanies companies = QCompanies.companies;
                    SQLQuery<?> query = new SQLQuery<Void>(conn, conf);
                    query.from(companies).where(companies.id.eq((long) i))
                        .select(companies.name).fetch();
                }
            }
        });
    }

    @Test
    public void querydsl12() throws Exception {
        Runner.run("qdsl by id (iterated)", new Benchmark() {
            @Override
            public void run(int times) throws Exception {
                for (int i = 0; i < times; i++) {
                    QCompanies companies = QCompanies.companies;
                    SQLQuery<?> query = new SQLQuery<Void>(conn, conf);
                    CloseableIterator<String> it = query.from(companies)
                            .where(companies.id.eq((long) i)).select(companies.name).iterate();
                    try {
                        while (it.hasNext()) {
                            it.next();
                        }
                    } finally {
                        it.close();
                    }
                }
            }
        });
    }

    @Test
    public void querydsl13() throws Exception {
        Runner.run("qdsl by id (result set access)", new Benchmark() {
            @Override
            public void run(int times) throws Exception {
                for (int i = 0; i < times; i++) {
                    QCompanies companies = QCompanies.companies;
                    SQLQuery<?> query = new SQLQuery<Void>(conn, conf);
                    ResultSet rs = query.select(companies.name).from(companies)
                            .where(companies.id.eq((long) i)).getResults();
                    try {
                        while (rs.next()) {
                            rs.getString(1);
                        }
                    } finally {
                        rs.close();
                    }
                }
            }
        });
    }

    @Test
    public void querydsl14() throws Exception {
        Runner.run("qdsl by id (no validation)", new Benchmark() {
            @Override
            public void run(int times) throws Exception {
                for (int i = 0; i < times; i++) {
                    QCompanies companies = QCompanies.companies;
                    SQLQuery<?> query = new SQLQuery<Void>(conn, conf, new DefaultQueryMetadata());
                    query.from(companies).where(companies.id.eq((long) i))
                        .select(companies.name).fetch();
                }
            }
        });
    }

    @Test
    public void querydsl15() throws Exception {
        Runner.run("qdsl by id (two cols)", new Benchmark() {
            @Override
            public void run(int times) throws Exception {
                for (int i = 0; i < times; i++) {
                    QCompanies companies = QCompanies.companies;
                    SQLQuery<?> query = new SQLQuery<Void>(conn, conf);
                    query.from(companies).where(companies.id.eq((long) i))
                        .select(companies.id, companies.name).fetch();
                }
            }
        });
    }

    @Test
    public void querydsl2() throws Exception {
        Runner.run("qdsl by name", new Benchmark() {
            @Override
            public void run(int times) throws Exception {
                for (int i = 0; i < times; i++) {
                    QCompanies companies = QCompanies.companies;
                    SQLQuery<?> query = new SQLQuery<Void>(conn, conf);
                    query.from(companies).where(companies.name.eq(String.valueOf(i)))
                        .select(companies.name).fetch();
                }
            }
        });
    }

    @Test
    public void querydsl22() throws Exception {
        Runner.run("qdsl by name (iterated)", new Benchmark() {
            @Override
            public void run(int times) throws Exception {
                for (int i = 0; i < times; i++) {
                    QCompanies companies = QCompanies.companies;
                    SQLQuery<?> query = new SQLQuery<Void>(conn, conf);
                    CloseableIterator<String> it = query.from(companies)
                            .where(companies.name.eq(String.valueOf(i)))
                            .select(companies.name).iterate();
                    try {
                        while (it.hasNext()) {
                            it.next();
                        }
                    } finally {
                        it.close();
                    }
                }
            }
        });
    }

    @Test
    public void querydsl23() throws Exception {
        Runner.run("qdsl by name (no validation)", new Benchmark() {
            @Override
            public void run(int times) throws Exception {
                for (int i = 0; i < times; i++) {
                    QCompanies companies = QCompanies.companies;
                    SQLQuery<?> query = new SQLQuery<Void>(conn, conf, new DefaultQueryMetadata());
                    query.from(companies)
                        .where(companies.name.eq(String.valueOf(i)))
                        .select(companies.name).fetch();
                }
            }
        });
    }

    @Test
    public void serialization() throws Exception {
        QCompanies companies = QCompanies.companies;
        final QueryMetadata md = new DefaultQueryMetadata();
        md.addJoin(JoinType.DEFAULT, companies);
        md.addWhere(companies.id.eq(1L));
        md.setProjection(companies.name);

        Runner.run("ser1", new Benchmark() {
            @Override
            public void run(int times) throws Exception {
                for (int i = 0; i < times; i++) {
                    SQLSerializer serializer = new SQLSerializer(conf);
                    serializer.serialize(md, false);
                    serializer.getConstants();
                    serializer.getConstantPaths();
                    assertNotNull(serializer.toString());
                }
            }
        });
    }
}
