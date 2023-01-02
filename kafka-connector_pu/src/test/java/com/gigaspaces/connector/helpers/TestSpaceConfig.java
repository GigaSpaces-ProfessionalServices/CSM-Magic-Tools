package com.gigaspaces.connector.helpers;//

import org.openspaces.core.config.annotation.AbstractSpaceBeansConfig;
import org.openspaces.core.space.AbstractSpaceFactoryBean;
import org.openspaces.core.space.EmbeddedSpaceFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;

public class TestSpaceConfig extends AbstractSpaceBeansConfig {
    public TestSpaceConfig() {
    }

    protected AbstractSpaceFactoryBean createSpaceFactoryBean() {
        EmbeddedSpaceFactoryBean factoryBean = new EmbeddedSpaceFactoryBean();
        this.configure(factoryBean);
        return factoryBean;
    }

    protected void configure(EmbeddedSpaceFactoryBean factoryBean) {
        factoryBean.setSpaceName(this.getSpaceName());
    }

    @Bean
    PlatformTransactionManager getTransactionManager() {
        return new PlatformTransactionManager() {
            @Override
            public TransactionStatus getTransaction(TransactionDefinition transactionDefinition) throws TransactionException {
                return null;
            }

            @Override
            public void commit(TransactionStatus transactionStatus) throws TransactionException {

            }

            @Override
            public void rollback(TransactionStatus transactionStatus) throws TransactionException {

            }
        };

    }


}
