package org.mybatis.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @author yezhaoxing
 * @since 2018/08/30
 */
@Data
@AllArgsConstructor
public class ColumnOverrideDto {

    private String columnName;// 表的列名

    private String javaProperty;// 列对应的属性名

    private String javaType;// 列对应的class类型
}
