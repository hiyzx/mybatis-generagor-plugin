package org.mybatis.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 * @author yezhaoxing
 * @since 2018/08/30
 */
@Data
@AllArgsConstructor
public class TableProperties {

    private String tableName;// 表名

    private String tableClassName;// 表对应的类名

    private List<ColumnOverrideDto> columnOverrides;
}
