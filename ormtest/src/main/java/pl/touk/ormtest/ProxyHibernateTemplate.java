package pl.touk.ormtest;

import org.hibernate.Filter;
import org.hibernate.HibernateException;
import org.hibernate.Interceptor;
import org.hibernate.LockMode;
import org.hibernate.ReplicationMode;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.DetachedCriteria;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.support.SQLExceptionTranslator;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.HibernateTemplate;

import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

class ProxyHibernateTemplate extends HibernateTemplate {
    private final ThreadLocal<HibernateTemplate> target;

    public ProxyHibernateTemplate(ThreadLocal<HibernateTemplate> hibernateTemplate) {
        target = hibernateTemplate;
    }

    @Override
    public void saveOrUpdate(Object entity) throws DataAccessException {
        getTarget().saveOrUpdate(entity);
    }

    @Override
    public void refresh(Object entity, LockMode lockMode) throws DataAccessException {
        getTarget().refresh(entity, lockMode);
    }

    @Override
    public boolean isAllowCreate() {
        return getTarget().isAllowCreate();
    }

    @Override
    public void delete(String entityName, Object entity, LockMode lockMode) throws DataAccessException {
        getTarget().delete(entityName, entity, lockMode);
    }

    @Override
    public void setQueryCacheRegion(String queryCacheRegion) {
        getTarget().setQueryCacheRegion(queryCacheRegion);
    }

    @Override
    public boolean isCheckWriteOperations() {
        return getTarget().isCheckWriteOperations();
    }

    @Override
    public Object load(Class entityClass, Serializable id) throws DataAccessException {
        return getTarget().load(entityClass, id);
    }

    @Override
    public void setMaxResults(int maxResults) {
        getTarget().setMaxResults(maxResults);
    }

    @Override
    public void setFetchSize(int fetchSize) {
        getTarget().setFetchSize(fetchSize);
    }

    @Override
    public void delete(Object entity, LockMode lockMode) throws DataAccessException {
        getTarget().delete(entity, lockMode);
    }

    @Override
    public Object get(Class entityClass, Serializable id) throws DataAccessException {
        return getTarget().get(entityClass, id);
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) {
        getTarget().setBeanFactory(beanFactory);
    }

    @Override
    public void setSessionFactory(SessionFactory sessionFactory) {
        getTarget().setSessionFactory(sessionFactory);
    }

    @Override
    public Object load(Class entityClass, Serializable id, LockMode lockMode) throws DataAccessException {
        return getTarget().load(entityClass, id, lockMode);
    }

    @Override
    public List findByNamedQuery(String queryName, Object[] values) throws DataAccessException {
        return getTarget().findByNamedQuery(queryName, values);
    }

    @Override
    public void deleteAll(Collection entities) throws DataAccessException {
        getTarget().deleteAll(entities);
    }

    @Override
    public List find(String queryString) throws DataAccessException {
        return getTarget().find(queryString);
    }

    @Override
    public SessionFactory getSessionFactory() {
        return getTarget().getSessionFactory();
    }

    @Override
    public Iterator iterate(String queryString, Object[] values) throws DataAccessException {
        return getTarget().iterate(queryString, values);
    }

    @Override
    public String getQueryCacheRegion() {
        return getTarget().getQueryCacheRegion();
    }

    @Override
    public List findByExample(Object exampleEntity) throws DataAccessException {
        return getTarget().findByExample(exampleEntity);
    }

    @Override
    public int getFlushMode() {
        return getTarget().getFlushMode();
    }

    @Override
    public int bulkUpdate(String queryString, Object value) throws DataAccessException {
        return getTarget().bulkUpdate(queryString, value);
    }

    @Override
    public void afterPropertiesSet() {
        getTarget().afterPropertiesSet();
    }

    @Override
    public void saveOrUpdateAll(Collection entities) throws DataAccessException {
        getTarget().saveOrUpdateAll(entities);
    }

    @Override
    public int bulkUpdate(String queryString, Object[] values) throws DataAccessException {
        return getTarget().bulkUpdate(queryString, values);
    }

    @Override
    public void update(String entityName, Object entity, LockMode lockMode) throws DataAccessException {
        getTarget().update(entityName, entity, lockMode);
    }

    @Override
    public void setJdbcExceptionTranslator(SQLExceptionTranslator jdbcExceptionTranslator) {
        getTarget().setJdbcExceptionTranslator(jdbcExceptionTranslator);
    }

    @Override
    public SQLExceptionTranslator getJdbcExceptionTranslator() {
        return getTarget().getJdbcExceptionTranslator();
    }

    @Override
    public void persist(String entityName, Object entity) throws DataAccessException {
        getTarget().persist(entityName, entity);
    }

    @Override
    public List findByExample(String entityName, Object exampleEntity, int firstResult, int maxResults) throws DataAccessException {
        return getTarget().findByExample(entityName, exampleEntity, firstResult, maxResults);
    }

    @Override
    public Iterator iterate(String queryString) throws DataAccessException {
        return getTarget().iterate(queryString);
    }

    @Override
    public Serializable save(String entityName, Object entity) throws DataAccessException {
        return getTarget().save(entityName, entity);
    }

    @Override
    public List findByNamedQuery(String queryName, Object value) throws DataAccessException {
        return getTarget().findByNamedQuery(queryName, value);
    }

    @Override
    public Object executeWithNativeSession(HibernateCallback action) {
        return getTarget().executeWithNativeSession(action);
    }

    @Override
    public String[] getFilterNames() {
        return getTarget().getFilterNames();
    }

    @Override
    public List findByNamedParam(String queryString, String paramName, Object value) throws DataAccessException {
        return getTarget().findByNamedParam(queryString, paramName, value);
    }

    @Override
    public void delete(Object entity) throws DataAccessException {
        getTarget().delete(entity);
    }

    @Override
    public int getMaxResults() {
        return getTarget().getMaxResults();
    }

    @Override
    public List find(String queryString, Object value) throws DataAccessException {
        return getTarget().find(queryString, value);
    }

    @Override
    public void setFlushMode(int flushMode) {
        getTarget().setFlushMode(flushMode);
    }

    @Override
    public Object executeWithNewSession(HibernateCallback action) {
        return getTarget().executeWithNewSession(action);
    }

    @Override
    public void update(String entityName, Object entity) throws DataAccessException {
        getTarget().update(entityName, entity);
    }

    @Override
    public void clear() throws DataAccessException {
        getTarget().clear();
    }

    @Override
    public void delete(String entityName, Object entity) throws DataAccessException {
        getTarget().delete(entityName, entity);
    }

    @Override
    public Object get(String entityName, Serializable id, LockMode lockMode) throws DataAccessException {
        return getTarget().get(entityName, id, lockMode);
    }

    @Override
    public void update(Object entity) throws DataAccessException {
        getTarget().update(entity);
    }

    @Override
    public void update(Object entity, LockMode lockMode) throws DataAccessException {
        getTarget().update(entity, lockMode);
    }

    @Override
    public Object get(String entityName, Serializable id) throws DataAccessException {
        return getTarget().get(entityName, id);
    }

    @Override
    public List findByCriteria(DetachedCriteria criteria, int firstResult, int maxResults) throws DataAccessException {
        return getTarget().findByCriteria(criteria, firstResult, maxResults);
    }

    @Override
    public void persist(Object entity) throws DataAccessException {
        getTarget().persist(entity);
    }

    @Override
    public void initialize(Object proxy) throws DataAccessException {
        getTarget().initialize(proxy);
    }

    @Override
    public List findByNamedQueryAndNamedParam(String queryName, String paramName, Object value) throws DataAccessException {
        return getTarget().findByNamedQueryAndNamedParam(queryName, paramName, value);
    }

    @Override
    public void closeIterator(Iterator it) throws DataAccessException {
        getTarget().closeIterator(it);
    }

    @Override
    public void replicate(Object entity, ReplicationMode replicationMode) throws DataAccessException {
        getTarget().replicate(entity, replicationMode);
    }

    @Override
    public Object merge(Object entity) throws DataAccessException {
        return getTarget().merge(entity);
    }

    @Override
    public Iterator iterate(String queryString, Object value) throws DataAccessException {
        return getTarget().iterate(queryString, value);
    }

    @Override
    public void lock(String entityName, Object entity, LockMode lockMode) throws DataAccessException {
        getTarget().lock(entityName, entity, lockMode);
    }

    @Override
    public int getFetchSize() {
        return getTarget().getFetchSize();
    }

    @Override
    public DataAccessException convertHibernateAccessException(HibernateException ex) {
        return getTarget().convertHibernateAccessException(ex);
    }

    @Override
    public void setFilterNames(String[] filterNames) {
        getTarget().setFilterNames(filterNames);
    }

    @Override
    public Serializable save(Object entity) throws DataAccessException {
        return getTarget().save(entity);
    }

    @Override
    public List findByNamedQueryAndValueBean(String queryName, Object valueBean) throws DataAccessException {
        return getTarget().findByNamedQueryAndValueBean(queryName, valueBean);
    }

    @Override
    public void refresh(Object entity) throws DataAccessException {
        getTarget().refresh(entity);
    }

    @Override
    public List findByCriteria(DetachedCriteria criteria) throws DataAccessException {
        return getTarget().findByCriteria(criteria);
    }

    @Override
    public int bulkUpdate(String queryString) throws DataAccessException {
        return getTarget().bulkUpdate(queryString);
    }

    @Override
    public List loadAll(Class entityClass) throws DataAccessException {
        return getTarget().loadAll(entityClass);
    }

    @Override
    public void lock(Object entity, LockMode lockMode) throws DataAccessException {
        getTarget().lock(entity, lockMode);
    }

    @Override
    public void setAllowCreate(boolean allowCreate) {
        getTarget().setAllowCreate(allowCreate);
    }

    @Override
    public Object load(String entityName, Serializable id) throws DataAccessException {
        return getTarget().load(entityName, id);
    }

    @Override
    public List findByExample(Object exampleEntity, int firstResult, int maxResults) throws DataAccessException {
        return getTarget().findByExample(exampleEntity, firstResult, maxResults);
    }

    @Override
    public Object execute(HibernateCallback action, boolean enforceNativeSession) throws DataAccessException {
        return getTarget().execute(action, enforceNativeSession);
    }

    @Override
    public void evict(Object entity) throws DataAccessException {
        getTarget().evict(entity);
    }

    @Override
    public List findByNamedParam(String queryString, String[] paramNames, Object[] values) throws DataAccessException {
        return getTarget().findByNamedParam(queryString, paramNames, values);
    }

    @Override
    public void saveOrUpdate(String entityName, Object entity) throws DataAccessException {
        getTarget().saveOrUpdate(entityName, entity);
    }

    @Override
    public Filter enableFilter(String filterName) throws IllegalStateException {
        return getTarget().enableFilter(filterName);
    }

    @Override
    public List findByNamedQuery(String queryName) throws DataAccessException {
        return getTarget().findByNamedQuery(queryName);
    }

    @Override
    public List executeFind(HibernateCallback action) throws DataAccessException {
        return getTarget().executeFind(action);
    }

    @Override
    public boolean contains(Object entity) throws DataAccessException {
        return getTarget().contains(entity);
    }

    @Override
    public void setFilterName(String filter) {
        getTarget().setFilterName(filter);
    }

    @Override
    public List find(String queryString, Object[] values) throws DataAccessException {
        return getTarget().find(queryString, values);
    }

    @Override
    public boolean isExposeNativeSession() {
        return getTarget().isExposeNativeSession();
    }

    @Override
    public void setFlushModeName(String constantName) {
        getTarget().setFlushModeName(constantName);
    }

    @Override
    public Object load(String entityName, Serializable id, LockMode lockMode) throws DataAccessException {
        return getTarget().load(entityName, id, lockMode);
    }

    @Override
    public void setAlwaysUseNewSession(boolean alwaysUseNewSession) {
        getTarget().setAlwaysUseNewSession(alwaysUseNewSession);
    }

    @Override
    public Object merge(String entityName, Object entity) throws DataAccessException {
        return getTarget().merge(entityName, entity);
    }

    @Override
    public void setEntityInterceptor(Interceptor entityInterceptor) {
        getTarget().setEntityInterceptor(entityInterceptor);
    }

    @Override
    public void setExposeNativeSession(boolean exposeNativeSession) {
        getTarget().setExposeNativeSession(exposeNativeSession);
    }

    @Override
    public void load(Object entity, Serializable id) throws DataAccessException {
        getTarget().load(entity, id);
    }

    @Override
    public List findByExample(String entityName, Object exampleEntity) throws DataAccessException {
        return getTarget().findByExample(entityName, exampleEntity);
    }

    @Override
    public List findByValueBean(String queryString, Object valueBean) throws DataAccessException {
        return getTarget().findByValueBean(queryString, valueBean);
    }

    @Override
    public boolean isAlwaysUseNewSession() {
        return getTarget().isAlwaysUseNewSession();
    }

    @Override
    public void setCacheQueries(boolean cacheQueries) {
        getTarget().setCacheQueries(cacheQueries);
    }

    @Override
    public void replicate(String entityName, Object entity, ReplicationMode replicationMode) throws DataAccessException {
        getTarget().replicate(entityName, entity, replicationMode);
    }

    @Override
    public Interceptor getEntityInterceptor() throws IllegalStateException, BeansException {
        return getTarget().getEntityInterceptor();
    }

    @Override
    public Object execute(HibernateCallback action) throws DataAccessException {
        return getTarget().execute(action);
    }

    @Override
    public List findByNamedQueryAndNamedParam(String queryName, String[] paramNames, Object[] values) throws DataAccessException {
        return getTarget().findByNamedQueryAndNamedParam(queryName, paramNames, values);
    }

    @Override
    public void setEntityInterceptorBeanName(String entityInterceptorBeanName) {
        getTarget().setEntityInterceptorBeanName(entityInterceptorBeanName);
    }

    @Override
    public void flush() throws DataAccessException {
        getTarget().flush();
    }

    @Override
    public void setCheckWriteOperations(boolean checkWriteOperations) {
        getTarget().setCheckWriteOperations(checkWriteOperations);
    }

    @Override
    public boolean isCacheQueries() {
        return getTarget().isCacheQueries();
    }

    @Override
    public Object get(Class entityClass, Serializable id, LockMode lockMode) throws DataAccessException {
        return getTarget().get(entityClass, id, lockMode);
    }

    private HibernateTemplate getTarget() {
        HibernateTemplate result = target.get();
        if (result != null) {
            return result;
        } else {
            throw new IllegalStateException("hibernate template not initialized");
        }
    }
}
