package com.daimler.sechub.pds.job;

import static com.daimler.sechub.pds.job.PDSJob.*;
import static com.daimler.sechub.pds.job.PDSJobStatusState.*;

import java.util.List;
import java.util.Optional;

import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

public class PDSJobRepositoryImpl implements PDSJobRepositoryCustom {

    /* @formatter:off */
	public static final String JPQL_STRING_SELECT_BY_EXECUTION_STATE = 
			"select j from "+CLASS_NAME+" j"+
					" where j."+PROPERTY_STATE+" = :"+PROPERTY_STATE +
					" order by j."+PROPERTY_CREATED;
	/* @formatter:on */

    @PersistenceContext
    private EntityManager em;

    @Override
    public Optional<PDSJob> findNextJobToExecute() {

        Query query = em.createQuery(JPQL_STRING_SELECT_BY_EXECUTION_STATE);
        query.setParameter(PROPERTY_STATE, READY_TO_START);
        query.setMaxResults(1);
        // we use OPTIMISTIC_FORCE_INCREMENT write lock - so only one POD will be able
        // to execute next job...
        // see https://www.baeldung.com/jpa-pessimistic-locking
        query.setLockMode(LockModeType.OPTIMISTIC_FORCE_INCREMENT);

        List<?> list = query.getResultList();
        Object singleResult = null;
        if (! list.isEmpty()) {
            singleResult=list.iterator().next();
        }
        return Optional.ofNullable((PDSJob) singleResult);
    }

}