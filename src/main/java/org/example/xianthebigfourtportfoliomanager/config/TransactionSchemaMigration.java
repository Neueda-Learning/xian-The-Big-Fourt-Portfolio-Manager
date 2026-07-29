package org.example.xianthebigfourtportfoliomanager.config;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

@Component
@DependsOn("databaseBootstrapInitializer")
public class TransactionSchemaMigration {

    private final JdbcTemplate jdbcTemplate;

    public TransactionSchemaMigration(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void migrate() {
        if (!tableExists("transaction")) {
            return;
        }

        if (!columnExists("transaction", "portfolio_id")) {
            jdbcTemplate.execute("alter table `transaction` add column portfolio_id int null after id");
        }

        if (!holdingIdNullable()) {
            jdbcTemplate.execute("alter table `transaction` modify column holding_id int null");
        }

        jdbcTemplate.execute("""
                update `transaction` t
                inner join holding h on h.id = t.holding_id
                set t.portfolio_id = h.portfolio_id
                where t.portfolio_id is null and t.holding_id is not null
                """);

        jdbcTemplate.execute("""
            update `transaction` t
            inner join holding h on h.id = t.holding_id
            set t.portfolio_id = h.portfolio_id,
                t.holding_id = null
            where h.asset_type = 'CASH' or upper(coalesce(h.ticker, '')) = 'CASH'
            """);

        jdbcTemplate.execute("""
            delete from holding
            where asset_type = 'CASH' or upper(coalesce(ticker, '')) = 'CASH'
            """);

        if (!portfolioFkExists()) {
            jdbcTemplate.execute("alter table `transaction` add constraint fk_transaction_portfolio foreign key (portfolio_id) references portfolio(id) on delete cascade");
        }

        if (!indexExists("transaction", "idx_transaction_portfolio_id")) {
            jdbcTemplate.execute("create index idx_transaction_portfolio_id on `transaction`(portfolio_id)");
        }
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

    private boolean columnExists(String tableName, String columnName) {
        Integer count = jdbcTemplate.queryForObject(
                """
                        select count(*)
                        from information_schema.columns
                        where table_schema = database()
                          and table_name = ?
                          and column_name = ?
                        """,
                Integer.class,
                tableName,
                columnName
        );
        return count != null && count > 0;
    }

    private boolean holdingIdNullable() {
        String nullable = jdbcTemplate.queryForObject(
                """
                        select is_nullable
                        from information_schema.columns
                        where table_schema = database()
                          and table_name = 'transaction'
                          and column_name = 'holding_id'
                        """,
                String.class
        );
        return "YES".equalsIgnoreCase(nullable);
    }

    private boolean portfolioFkExists() {
        Integer count = jdbcTemplate.queryForObject(
                """
                        select count(*)
                        from information_schema.key_column_usage
                        where table_schema = database()
                          and table_name = 'transaction'
                          and column_name = 'portfolio_id'
                          and referenced_table_name = 'portfolio'
                        """,
                Integer.class
        );
        return count != null && count > 0;
    }

    private boolean indexExists(String tableName, String indexName) {
        Integer count = jdbcTemplate.queryForObject(
                """
                        select count(*)
                        from information_schema.statistics
                        where table_schema = database()
                          and table_name = ?
                          and index_name = ?
                        """,
                Integer.class,
                tableName,
                indexName
        );
        return count != null && count > 0;
    }
}