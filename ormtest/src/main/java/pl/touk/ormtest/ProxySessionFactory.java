package pl.touk.ormtest;

import org.hibernate.HibernateException;
import org.hibernate.Interceptor;
import org.hibernate.SessionFactory;
import org.hibernate.StatelessSession;
import org.hibernate.classic.Session;
import org.hibernate.engine.FilterDefinition;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.metadata.CollectionMetadata;
import org.hibernate.stat.Statistics;

import javax.naming.NamingException;
import javax.naming.Reference;
import java.io.Serializable;
import java.sql.Connection;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

class ProxySessionFactory implements SessionFactory {
    private final ThreadLocal<SessionFactory> target;

    public ProxySessionFactory(ThreadLocal<SessionFactory> target) {
        this.target = checkNotNull(target, "target must not be null");
    }

    @Override
    public Session openSession(Connection connection) {
        return getTarget().openSession(connection);
    }

    @Override
    public Session openSession(Interceptor interceptor) throws HibernateException {
        return getTarget().openSession(interceptor);
    }

    @Override
    public Session openSession(Connection connection, Interceptor interceptor) {
        return getTarget().openSession(connection, interceptor);
    }

    @Override
    public Session openSession() throws HibernateException {
        return getTarget().openSession();
    }

    @Override
    public Session getCurrentSession() throws HibernateException {
        return getTarget().getCurrentSession();
    }

    @Override
    public ClassMetadata getClassMetadata(Class persistentClass) throws HibernateException {
        return getTarget().getClassMetadata(persistentClass);
    }

    @Override
    public ClassMetadata getClassMetadata(String entityName) throws HibernateException {
        return getTarget().getClassMetadata(entityName);
    }

    @Override
    public CollectionMetadata getCollectionMetadata(String roleName) throws HibernateException {
        return getTarget().getCollectionMetadata(roleName);
    }

    @Override
    public Map getAllClassMetadata() throws HibernateException {
        return getTarget().getAllClassMetadata();
    }

    @Override
    public Map getAllCollectionMetadata() throws HibernateException {
        return getTarget().getAllCollectionMetadata();
    }

    @Override
    public Statistics getStatistics() {
        return getTarget().getStatistics();
    }

    @Override
    public void close() throws HibernateException {
        getTarget().close();
    }

    @Override
    public boolean isClosed() {
        return getTarget().isClosed();
    }

    @Override
    public void evict(Class persistentClass) throws HibernateException {
        getTarget().evict(persistentClass);
    }

    @Override
    public void evict(Class persistentClass, Serializable id) throws HibernateException {
        getTarget().evict(persistentClass, id);
    }

    @Override
    public void evictEntity(String entityName) throws HibernateException {
        getTarget().evictEntity(entityName);
    }

    @Override
    public void evictEntity(String entityName, Serializable id) throws HibernateException {
        getTarget().evictEntity(entityName, id);
    }

    @Override
    public void evictCollection(String roleName) throws HibernateException {
        getTarget().evictCollection(roleName);
    }

    @Override
    public void evictCollection(String roleName, Serializable id) throws HibernateException {
        getTarget().evictCollection(roleName, id);
    }

    @Override
    public void evictQueries() throws HibernateException {
        getTarget().evictQueries();
    }

    @Override
    public void evictQueries(String cacheRegion) throws HibernateException {
        getTarget().evictQueries(cacheRegion);
    }

    @Override
    public StatelessSession openStatelessSession() {
        return getTarget().openStatelessSession();
    }

    @Override
    public StatelessSession openStatelessSession(Connection connection) {
        return getTarget().openStatelessSession(connection);
    }

    @Override
    public Set getDefinedFilterNames() {
        return getTarget().getDefinedFilterNames();
    }

    @Override
    public FilterDefinition getFilterDefinition(String filterName) throws HibernateException {
        return getTarget().getFilterDefinition(filterName);
    }

    @Override
    public Reference getReference() throws NamingException {
        return getTarget().getReference();
    }

    private SessionFactory getTarget() {
        SessionFactory result = target.get();
        if (result != null) {
            return result;
        } else {
            throw new IllegalStateException("session factory not initialized");
        }
    }
}
