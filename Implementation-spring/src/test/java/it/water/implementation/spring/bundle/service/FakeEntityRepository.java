package it.water.implementation.spring.bundle.service;

import it.water.core.api.model.PaginableResult;
import it.water.core.api.repository.query.Query;
import it.water.core.api.repository.query.QueryBuilder;
import it.water.core.api.repository.query.QueryOrder;
import it.water.core.interceptors.annotations.FrameworkComponent;

@FrameworkComponent
public class FakeEntityRepository implements it.water.implementation.spring.bundle.api.FakeEntityRepository {
    @Override
    public Class<FakeEntity> getEntityType() {
        return FakeEntity.class;
    }

    @Override
    public FakeEntity persist(FakeEntity fakeEntity) {
        return null;
    }

    @Override
    public FakeEntity persist(FakeEntity fakeEntity, Runnable runnable) {
        return null;
    }

    @Override
    public FakeEntity update(FakeEntity fakeEntity) {
        return null;
    }

    @Override
    public FakeEntity update(FakeEntity fakeEntity, Runnable runnable) {
        return null;
    }

    @Override
    public void remove(long l) {

    }

    @Override
    public void remove(long l, Runnable runnable) {

    }

    @Override
    public void remove(FakeEntity fakeEntity) {

    }

    @Override
    public void removeAllByIds(Iterable<Long> iterable) {

    }

    @Override
    public void removeAll(Iterable<FakeEntity> iterable) {

    }

    @Override
    public void removeAll() {

    }

    @Override
    public FakeEntity find(long l) {
        return null;
    }

    @Override
    public FakeEntity find(Query query) {
        return null;
    }

    @Override
    public FakeEntity find(String s) {
        return null;
    }

    @Override
    public PaginableResult<FakeEntity> findAll(int i, int i1, Query query, QueryOrder queryOrder) {
        return null;
    }

    @Override
    public long countAll(Query query) {
        return 0;
    }

    @Override
    public QueryBuilder getQueryBuilderInstance() {
        return null;
    }
}
