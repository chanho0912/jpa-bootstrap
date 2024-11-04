package persistence.entity;

import common.ReflectionFieldAccessUtils;
import jdbc.JdbcTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import persistence.meta.Metamodel;
import persistence.sql.definition.TableAssociationDefinition;
import persistence.sql.definition.TableDefinition;
import persistence.sql.dml.query.InsertQueryBuilder;
import persistence.sql.dml.query.UpdateQueryBuilder;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class EntityCollectionPersister {
    private static final InsertQueryBuilder insertQueryBuilder = new InsertQueryBuilder();
    private static final UpdateQueryBuilder updateQueryBuilder = new UpdateQueryBuilder();
    private final Logger logger = LoggerFactory.getLogger(EntityCollectionPersister.class);

    private final TableDefinition parentTableDefinition;
    private final TableDefinition elementTableDefinition;
    private final Metamodel metamodel;

    private final JdbcTemplate jdbcTemplate;
    private final boolean isEager;

    public EntityCollectionPersister(TableDefinition parentTableDefinition, TableDefinition elementTableDefinition,
                                     JdbcTemplate jdbcTemplate, Metamodel metamodel) {
        this.parentTableDefinition = parentTableDefinition;
        this.elementTableDefinition = elementTableDefinition;
        final TableAssociationDefinition association = parentTableDefinition.getAssociation(
                elementTableDefinition.getEntityClass());

        this.metamodel = metamodel;
        this.isEager = association.isEager();
        this.jdbcTemplate = jdbcTemplate;
    }

    public Collection<Object> insertCollection(Object parentEntity, TableAssociationDefinition association) {
        final List<Object> childEntities = new ArrayList<>();
        final Collection<?> associatedValues = parentTableDefinition.getIterableAssociatedValue(parentEntity, association);
        if (associatedValues instanceof Iterable<?> iterable) {
            iterable.forEach(entity -> {
                Object result = doInsert(entity);
                childEntities.add(result);
            });
        }

        childEntities.forEach(childEntity -> {
            final String sql = updateQueryBuilder.build(parentEntity, childEntity, parentTableDefinition, elementTableDefinition);
            jdbcTemplate.execute(sql);
        });
        return childEntities;
    }

    private Object doInsert(Object entity) {
        final String query = insertQueryBuilder.build(entity, metamodel);
        final Serializable id = jdbcTemplate.insertAndReturnKey(query);

        bindId(id, entity);
        return entity;
    }

    private void bindId(Serializable id, Object entity) {
        try {
            final Field idField = elementTableDefinition.getEntityClass()
                    .getDeclaredField(elementTableDefinition.getIdFieldName());

            ReflectionFieldAccessUtils.accessAndSet(entity, idField, id);
        } catch (ReflectiveOperationException e) {
            logger.error("Failed to copy row to {}", entity.getClass().getName(), e);
        }
    }

}
