package persistence.event.persist;

import database.DatabaseServer;
import database.H2;
import org.junit.jupiter.api.Test;
import persistence.entity.EntityKey;
import persistence.event.EventSource;
import persistence.fixtures.SimplePerson;
import persistence.meta.Metadata;
import persistence.meta.MetadataImpl;
import persistence.session.SessionFactoryImpl;
import persistence.session.ThreadLocalCurrentSessionContext;

import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

class DefaultPersistEventListenerTest {

    @Test
    void testOnPersist() throws SQLException {
        DatabaseServer databaseServer = new H2();
        Metadata metadata = new MetadataImpl(databaseServer);

        SessionFactoryImpl sessionFactory = new SessionFactoryImpl(new ThreadLocalCurrentSessionContext(), metadata);

        EventSource source = (EventSource) sessionFactory.openSession();
        DefaultPersistEventListener defaultPersistEventListener = new DefaultPersistEventListener();

        SimplePerson entity = new SimplePerson("John");
        PersistEvent persistEvent = PersistEvent.create(source, entity);
        defaultPersistEventListener.onPersist(persistEvent);

        EntityKey entityKey = new EntityKey(1L, SimplePerson.class);

        assertAll(
                () -> assertThat(source.getPersistenceContext().getEntity(entityKey)).isEqualTo(entity),
                () -> assertThat(source.getPersistenceContext().getDatabaseSnapshot(entityKey)).isNotNull(),
                () -> assertThat(source.getPersistenceContext().getEntityEntry(entityKey).isManaged()).isTrue()
        );

        sessionFactory.close();
    }
}
