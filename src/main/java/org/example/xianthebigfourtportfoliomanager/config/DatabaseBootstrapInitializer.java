package org.example.xianthebigfourtportfoliomanager.config;

import jakarta.annotation.PostConstruct;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.DatabasePopulatorUtils;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

@Component("databaseBootstrapInitializer")
public class DatabaseBootstrapInitializer {

    private final JdbcTemplate jdbcTemplate;
    private final DataSource dataSource;

    public DatabaseBootstrapInitializer(JdbcTemplate jdbcTemplate, DataSource dataSource) {
        this.jdbcTemplate = jdbcTemplate;
        this.dataSource = dataSource;
    }

    @PostConstruct
    public void bootstrap() {
        boolean portfolioExists = tableExists("portfolio");
        boolean holdingExists = tableExists("holding");
        boolean transactionExists = tableExists("transaction");
        boolean snapshotExists = tableExists("portfolio_snapshot");
        boolean priceHistoryExists = tableExists("price_history");

        boolean missingCoreTables = !portfolioExists || !holdingExists || !transactionExists || !snapshotExists || !priceHistoryExists;
        if (missingCoreTables) {
            runScript("schema.sql");
        }

        if (!tableExists("portfolio") || isPortfolioEmpty()) {
            runScript("data.sql");
        }
    }

    private void runScript(String scriptName) {
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator(new ClassPathResource(scriptName));
        populator.setContinueOnError(false);
        populator.setSeparator(";");
        DatabasePopulatorUtils.execute(populator, dataSource);
    }

    private boolean tableExists(String tableName) {
        Integer count = jdbcTemplate.queryForObject(
                """
                        select count(*)
                        from information_schema.tables
                        where table_schema = database()
                          and table_name = ?
                        """,
                Integer.class,
                tableName
        );
        return count != null && count > 0;
    }

    private boolean isPortfolioEmpty() {
        Integer count = jdbcTemplate.queryForObject("select count(*) from portfolio", Integer.class);
        return count == null || count == 0;
    }
}