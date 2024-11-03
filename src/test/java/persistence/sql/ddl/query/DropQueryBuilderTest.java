package persistence.sql.ddl.query;

import domain.Person;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DropQueryBuilderTest {

    @Test
    @DisplayName("should create a DROP TABLE query")
    void build() {
        DropQueryBuilder dropQueryBuilder = new DropQueryBuilder(Person.class);
        String query = dropQueryBuilder.build();

        // Then
        assertThat(query).isEqualTo("DROP TABLE users if exists;");
    }
}