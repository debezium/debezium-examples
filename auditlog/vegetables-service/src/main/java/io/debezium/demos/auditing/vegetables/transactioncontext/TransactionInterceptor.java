package io.debezium.demos.auditing.vegetables.transactioncontext;

import java.math.BigInteger;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;
import javax.persistence.EntityManager;

import io.debezium.demos.auditing.vegetables.rest.RequestContext;

@Interceptor
@Priority(value = Interceptor.Priority.APPLICATION + 100)
@TransactionContext(useCase = "")
public class TransactionInterceptor {

    @Inject
    EntityManager entityManager;

    @Inject
    RequestContext requestContext;

   @AroundInvoke
   public Object manageTransaction(InvocationContext ctx) throws Exception {
       Object result = ctx.proceed();
       BigInteger txtId = (BigInteger) entityManager.createNativeQuery("SELECT txid_current()").getSingleResult();
       String useCase = ctx.getMethod().getAnnotation(TransactionContext.class).useCase();

       TransactionContextData context = new TransactionContextData();

       context.transactionId = txtId.longValueExact();
       context.userName = requestContext.userName;
       context.clientDate = requestContext.date;
       context.usecase = useCase;

       entityManager.persist(context);

       return result;
   }
}
