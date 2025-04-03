package it.water.implementation.spring.bundle.service;

import it.water.core.api.model.PaginableResult;
import it.water.core.api.repository.query.Query;
import it.water.core.api.repository.query.QueryBuilder;
import it.water.core.api.repository.query.QueryOrder;
import it.water.core.interceptors.annotations.FrameworkComponent;
import it.water.implementation.spring.bundle.api.FakeEntitySystemApi;

@FrameworkComponent
public class FakeEntitySystemServiceImpl implements FakeEntitySystemApi {

    @Override
    public FakeEntity save(FakeEntity fakeEntity) {
        return null;
    }

    @Override
    public FakeEntity update(FakeEntity fakeEntity) {
        return null;
    }

    @Override
    public void remove(long l) {

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
    public PaginableResult<FakeEntity> findAll(Query query, int i, int i1, QueryOrder queryOrder) {
        return null;
    }

    @Override
    public long countAll(Query query) {
        return 0;
    }

    @Override
    public Class<FakeEntity> getEntityType() {
        return FakeEntity.class;
    }

    @Override
    public QueryBuilder getQueryBuilderInstance() {
        return null;
    }
}
