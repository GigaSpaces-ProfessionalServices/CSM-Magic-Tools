package com.gigaspaces.connector.data;

import com.gigaspaces.connector.config.DataPipelineConfig;
import com.gigaspaces.connector.plugins.data.Parser;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ConditionMatcher {
    public boolean anyConditionMatch(MessageContainer messageContainer, List<DataPipelineConfig.Condition> conditions) {
        if (conditions == null) return false;

        for (DataPipelineConfig.Condition c : conditions)
            if (conditionMatch(messageContainer, c)) return true;

        return false;
    }

    private boolean conditionMatch(MessageContainer messageContainer, DataPipelineConfig.Condition condition) {
        String selector = condition.getSelector();
        Object conditionValue = condition.getValue();

        Parser valueParser = messageContainer.getValueParser();
        if (!valueParser.containsValue(selector))
            return false;

        Object actualValue = valueParser.getValue(selector);

        // TODO: 21/11/2021 what if actualValue is null and condition is "=null"?
        // TODO: 21/11/2021 how to do it w/o converting to string?
        String actualValueStr = PrimitiveValueToStringConverter.convert(actualValue);
        String conditionValueStr = PrimitiveValueToStringConverter.convert(conditionValue);

        return conditionValueStr.equals(actualValueStr);
    }

}
