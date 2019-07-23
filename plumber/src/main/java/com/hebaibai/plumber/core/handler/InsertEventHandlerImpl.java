package com.hebaibai.plumber.core.handler;

import com.github.shyiko.mysql.binlog.event.EventData;
import com.github.shyiko.mysql.binlog.event.EventHeader;
import com.github.shyiko.mysql.binlog.event.EventType;
import com.hebaibai.plumber.ConsumerAddress;
import com.hebaibai.plumber.core.utils.EventDataUtils;
import io.vertx.core.eventbus.EventBus;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * 插入事件处理器
 *
 * @author hjx
 */
@Slf4j
public class InsertEventHandlerImpl extends AbstractEventHandler implements EventHandler {

    @Override
    public boolean support(EventHeader eventHeader, String dataBaseName, String tableName) {
        if (!status) {
            return false;
        }
        if (!EventType.isWrite(eventHeader.getEventType())) {
            return false;
        }
        return sourceDatabase.equals(dataBaseName) && sourceTable.equals(tableName);
    }

    @Override
    public void handle(EventBus eventBus, EventData data) {
        log.info("new event insert ... ");
        String[] rows = EventDataUtils.getInsertRows(data);
        List<String> columns = sourceTableMateData.getColumns();
        List<String> targetColumns = new ArrayList<>();
        List<String> targetColumnValues = new ArrayList<>();
        for (int i = 0; i < columns.size(); i++) {
            String sourceName = columns.get(i);
            if (!mapping.containsKey(sourceName)) {
                continue;
            }
            //目标字段
            String targetName = mapping.get(sourceName);
            if (targetName == null) {
                continue;
            }
            targetColumns.add("`" + targetName + "`");
            if (rows[i] == null) {
                targetColumnValues.add("null");
            } else {
                targetColumnValues.add("'" + rows[i] + "'");
            }
        }
        StringBuilder sqlBuilder = new StringBuilder();
        sqlBuilder.append("REPLACE INTO ");
        sqlBuilder.append(targetDatabase).append(".").append(targetTable);
        sqlBuilder.append(" ( ").append(String.join(", ", targetColumns));
        sqlBuilder.append(" ) VALUES ( ").append(String.join(", ", targetColumnValues));
        sqlBuilder.append(");");
        String sql = sqlBuilder.toString();
        eventBus.send(ConsumerAddress.EXECUTE_SQL_DELETE, sql);
    }


    @Override
    public String getName() {
        return "insert event hander";
    }
}