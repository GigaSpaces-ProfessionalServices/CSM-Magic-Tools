package com.gigaspaces.connector.cdc;

import com.gigaspaces.connector.config.ConfigurationException;
import com.gigaspaces.connector.config.Consts;
import com.gigaspaces.connector.config.DataPipelineConfig;
import com.gigaspaces.connector.data.ConditionMatcher;
import com.gigaspaces.connector.data.MessageContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import oshi.util.tuples.Pair;

import javax.annotation.PostConstruct;
import java.util.*;

@Component
//@Profile(Consts.CONNECTOR_MODE)
public class CdcOperationResolver {

    private static final Logger logger = LoggerFactory.getLogger(CdcOperationResolver.class);
    private List<Pair<List<DataPipelineConfig.Condition>, CdcOperation>> evaluationSteps = new ArrayList<>();
    @Autowired
    private DataPipelineConfig dataPipelineConfig;
    @Autowired
    private ConditionMatcher conditionMatcher;
    private boolean cdcEnabled = true;
    private Optional<CdcOperation> defaultCdcOperation = Optional.empty();
    private Map<CdcOperation, String> opToPropSelectorTemplate = new HashMap<>();

    @PostConstruct
    public void init() {
        DataPipelineConfig.Cdc cdcConfig = dataPipelineConfig.getCdc();

        if ((cdcConfig == null) || (cdcConfig.getOperations() == null)) {
            cdcEnabled = false;
            logger.info("CDC mode is not enabled.");
            return;
        }

        DataPipelineConfig.Operations operations = cdcConfig.getOperations();

        addEvaluationStep(CdcOperation.INSERT, operations.getInsert());
        addEvaluationStep(CdcOperation.UPDATE, operations.getUpdate());
        addEvaluationStep(CdcOperation.DELETE, operations.getDelete());
        logger.info("CDC mode enabled.");
    }

    // TODO: 26/11/2021 for test. find better way
    public void reinit(DataPipelineConfig dataPipelineConfig) {
        this.dataPipelineConfig = dataPipelineConfig;
        this.cdcEnabled = true;
        this.defaultCdcOperation = Optional.empty();
        this.evaluationSteps = new ArrayList<>();
        init();
    }

    private void addEvaluationStep(CdcOperation operation, DataPipelineConfig.Operation operationConfig) {
        if (operationConfig == null) return;
        opToPropSelectorTemplate.put(operation, operationConfig.getPropertiesSelectorTemplate());
        if (operationConfig.isDefaultOperation()) {
            if (defaultCdcOperation.isPresent())
                throw new ConfigurationException("Only one CDC operation at most can be configured as the default one.");
            defaultCdcOperation = Optional.of(operation);
        } else {
            Pair<List<DataPipelineConfig.Condition>, CdcOperation> evaluationStep =
                    new Pair<>(operationConfig.getConditions(), operation);
            evaluationSteps.add(evaluationStep);
        }
    }

    public Pair<CdcOperation, String> resolve(MessageContainer messageContainer) {
        if (!cdcEnabled) return new Pair<>(CdcOperation.INSERT, null);

        for (Pair<List<DataPipelineConfig.Condition>, CdcOperation> evalStep : evaluationSteps)
            if (conditionMatcher.anyConditionMatch(messageContainer, evalStep.getA())) {
                CdcOperation cdcOperation = evalStep.getB();
                return new Pair<>(cdcOperation, opToPropSelectorTemplate.get(cdcOperation));
            }

        if (defaultCdcOperation.isPresent()) {
            CdcOperation cdcOperation = defaultCdcOperation.get();
            return new Pair<>(cdcOperation, opToPropSelectorTemplate.get(cdcOperation));
        } else {
            throw new CdcException("No match to any of CDC operations");
        }
    }

    public DataPipelineConfig.IfNotExists getIfNotExists() {
        return dataPipelineConfig.getCdc().getOperations().getUpdate().getIfNotExists();
    }

    public DataPipelineConfig.IfExists getIfExists() {
        if (!cdcEnabled) return DataPipelineConfig.IfExists.override;

        DataPipelineConfig.IfExists ifExists =
                dataPipelineConfig.getCdc().getOperations().getInsert().getIfExists();

        return ifExists == null ? DataPipelineConfig.IfExists.override : ifExists;
    }
}
