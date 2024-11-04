package persistence.entity;

import jdbc.JdbcTemplate;
import persistence.meta.Metamodel;
import persistence.sql.definition.TableAssociationDefinition;
import persistence.sql.definition.TableDefinition;

import java.io.Serializable;
import java.util.Collection;
import java.util.function.Supplier;

public class EntityManagerImpl implements EntityManager {
    private final PersistenceContext persistenceContext;
    private final Metamodel metamodel;
    private final EntityLoader entityLoader;

    public EntityManagerImpl(JdbcTemplate jdbcTemplate,
                             PersistenceContext persistenceContext,
                             Metamodel metamodel) {

        this.persistenceContext = persistenceContext;
        this.metamodel = metamodel;
        this.entityLoader = new EntityLoader(jdbcTemplate, metamodel);
    }

    @Override
    public <T> T find(Class<T> clazz, Object id) {
        final EntityKey entityKey = new EntityKey((Long) id, clazz);
        final EntityEntry entityEntry = getEntityEntryOrDefault(entityKey, () -> EntityEntry.loading((Serializable) id));

        if (entityEntry.isManaged()) {
            return clazz.cast(persistenceContext.getEntity(entityKey));
        }

        if (entityEntry.isNotReadable()) {
            throw new IllegalArgumentException("Entity is not managed: " + clazz.getSimpleName());
        }

        final T loaded = entityLoader.loadEntity(clazz, entityKey);
        addEntityInContext(entityKey, loaded);
        addManagedEntityEntry(entityKey, entityEntry);
        return loaded;
    }

    private EntityEntry getEntityEntryOrDefault(EntityKey entityKey, Supplier<EntityEntry> defaultEntrySupplier) {
        final EntityEntry entityEntry = persistenceContext.getEntityEntry(entityKey);
        if (entityEntry == null) {
            return defaultEntrySupplier.get();
        }

        return entityEntry;
    }

    @Override
    public void persist(Object entity) {
        final EntityPersister entityPersister = metamodel.getEntityPersister(entity.getClass());
        if (entityPersister.hasId(entity)) {
            final EntityEntry entityEntry = persistenceContext.getEntityEntry(
                    new EntityKey(entityPersister.getEntityId(entity), entity.getClass())
            );

            if (entityEntry == null) {
                throw new IllegalArgumentException("No Entity Entry with id: " + entityPersister.getEntityId(entity));
            }

            if (entityEntry.isManaged()) {
                return;
            }

            throw new IllegalArgumentException("Entity already persisted");
        }

        persistEntity(entity, entityPersister);
    }

    private void persistEntity(Object entity, EntityPersister entityPersister) {
        entityPersister.insert(entity);

        managePersistEntity(entity, entityPersister);

        for (TableAssociationDefinition association : entityPersister.getCollectionAssociations()) {
            final EntityCollectionPersister entityCollectionPersister = metamodel.getEntityCollectionPersister(association);
            final Collection<Object> childEntities = entityCollectionPersister.insertCollection(entity, association);
            childEntities.forEach(child -> {
                managePersistEntity(child, metamodel.getEntityPersister(child.getClass()));
            });
        }
    }

    private void managePersistEntity(Object entity, EntityPersister entityPersister) {
        final EntityEntry entityEntry = EntityEntry.inSaving();
        final EntityKey entityKey = new EntityKey(entityPersister.getEntityId(entity), entity.getClass());

        addEntityInContext(entityKey, entity);
        addManagedEntityEntry(entityKey, entityEntry);
    }

    @Override
    public void remove(Object entity) {
        final EntityPersister entityPersister = metamodel.getEntityPersister(entity.getClass());
        final EntityKey entityKey = new EntityKey(entityPersister.getEntityId(entity), entity.getClass());
        final EntityEntry entityEntry = persistenceContext.getEntityEntry(entityKey);
        checkManagedEntity(entity, entityEntry);

        entityEntry.updateStatus(Status.DELETED);
        entityPersister.delete(entity);
        persistenceContext.removeEntity(entityKey);
    }

    @Override
    public <T> T merge(T entity) {
        final EntityPersister entityPersister = metamodel.getEntityPersister(entity.getClass());
        final EntityKey entityKey = new EntityKey(entityPersister.getEntityId(entity), entity.getClass());
        final EntityEntry entityEntry = persistenceContext.getEntityEntry(entityKey);
        checkManagedEntity(entity, entityEntry);

        final EntitySnapshot entitySnapshot = persistenceContext.getDatabaseSnapshot(entityKey);
        if (entitySnapshot.hasDirtyColumns(entity, metamodel.getTableDefinition(entity.getClass()))) {
            entityPersister.update(entity);
        }

        addEntityInContext(entityKey, entity);
        addManagedEntityEntry(entityKey, entityEntry);
        return entity;
    }

    @Override
    public void clear() {
        persistenceContext.clear();
    }

    private void checkManagedEntity(Object entity, EntityEntry entityEntry) {
        if (entityEntry == null) {
            throw new IllegalStateException("Can not find entity in persistence context: "
                    + entity.getClass().getSimpleName());
        }

        if (!entityEntry.isManaged()) {
            throw new IllegalArgumentException("Detached entity can not be merged: "
                    + entity.getClass().getSimpleName());
        }
    }

    private void addEntityInContext(EntityKey entityKey, Object entity) {
        final TableDefinition tableDefinition = metamodel.getTableDefinition(entity.getClass());

        persistenceContext.addEntity(entityKey, entity);
        persistenceContext.addDatabaseSnapshot(entityKey, entity, tableDefinition);
    }

    private void addManagedEntityEntry(EntityKey entityKey, EntityEntry entityEntry) {
        entityEntry.updateStatus(Status.MANAGED);
        persistenceContext.addEntry(entityKey, entityEntry);
    }

}
