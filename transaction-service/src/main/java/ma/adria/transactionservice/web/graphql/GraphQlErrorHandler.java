package ma.adria.transactionservice.web.graphql;

import graphql.GraphQLError;
import ma.adria.transactionservice.client.AccountServiceUnavailableException;
import ma.adria.transactionservice.service.TransactionNotFoundException;
import org.springframework.graphql.data.method.annotation.GraphQlExceptionHandler;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.web.bind.annotation.ControllerAdvice;

@ControllerAdvice
public class GraphQlErrorHandler {

    @GraphQlExceptionHandler
    public GraphQLError handleNotFound(TransactionNotFoundException e) {
        return GraphQLError.newError().errorType(ErrorType.NOT_FOUND).message(e.getMessage()).build();
    }

    @GraphQlExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public GraphQLError handleBadRequest(RuntimeException e) {
        return GraphQLError.newError().errorType(ErrorType.BAD_REQUEST).message(e.getMessage()).build();
    }

    @GraphQlExceptionHandler
    public GraphQLError handleUnavailable(AccountServiceUnavailableException e) {
        return GraphQLError.newError().errorType(ErrorType.INTERNAL_ERROR).message(e.getMessage()).build();
    }
}