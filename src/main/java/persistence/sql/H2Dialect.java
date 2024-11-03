package persistence.sql;

import persistence.sql.definition.ColumnDefinitionAware;

import java.util.Map;

public class H2Dialect implements Dialect {
    private final Map<SqlType, String> typeMap = Map.of(
            SqlType.VARCHAR, "VARCHAR",
            SqlType.BIGINT, "BIGINT",
            SqlType.INTEGER, "INTEGER"
    );

    @Override
    public String translateType(ColumnDefinitionAware columnDefinition) {
        return switch (columnDefinition.getSqlType()) {
            case VARCHAR -> typeMap.get(SqlType.VARCHAR) + "(" + columnDefinition.getLength() + ")";
            case BIGINT -> typeMap.get(SqlType.BIGINT);
            case INTEGER -> typeMap.get(SqlType.INTEGER);
            case ARRAY -> throw new UnsupportedOperationException("ARRAY type is not supported by H2");
        };
    }
}
