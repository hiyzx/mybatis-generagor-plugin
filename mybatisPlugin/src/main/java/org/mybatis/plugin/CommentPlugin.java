package org.mybatis.plugin;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.mybatis.generator.api.FullyQualifiedTable;
import org.mybatis.generator.api.IntrospectedColumn;
import org.mybatis.generator.api.IntrospectedTable;
import org.mybatis.generator.api.PluginAdapter;
import org.mybatis.generator.api.dom.java.Field;
import org.mybatis.generator.api.dom.java.FullyQualifiedJavaType;
import org.mybatis.generator.api.dom.java.TopLevelClass;
import org.mybatis.generator.internal.db.ConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @see https://github.com/shan-ren/mybatis-generator-plugins-auto-comments/blob
 *      /master/CommentPlugin.java
 */
public class CommentPlugin extends PluginAdapter {
    private static final Logger LOG = LoggerFactory.getLogger(CommentPlugin.class);

    @Override
    public boolean modelFieldGenerated(Field field, TopLevelClass topLevelClass, IntrospectedColumn introspectedColumn,
            IntrospectedTable introspectedTable, ModelClassType modelClassType) {
        String remark = introspectedColumn.getRemarks();
        if (StringUtils.isNotEmpty(remark)) {
            field.addJavaDocLine(String.format("@ApiModelProperty(value = \"%s\")", remark));
        }
        return true;
    }

    @Override
    public boolean modelBaseRecordClassGenerated(TopLevelClass topLevelClass, IntrospectedTable introspectedTable) {
        String remarks = "";

        FullyQualifiedTable table = introspectedTable.getFullyQualifiedTable();
        try {
            Connection connection = ConnectionFactory.getInstance()
                    .getConnection(context.getJdbcConnectionConfiguration());
            ResultSet rs = connection.getMetaData().getTables(table.getIntrospectedCatalog(),
                    table.getIntrospectedSchema(), table.getIntrospectedTableName(), null);

            if (null != rs && rs.next()) {
                remarks = rs.getString("REMARKS");
            }
            closeConnection(connection, rs);
        } catch (SQLException e) {
            LOG.error(e.getMessage(), e);
        }

        if (StringUtils.isNotEmpty(remarks)) {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-M-d");
            topLevelClass.addJavaDocLine("/**");
            topLevelClass.addJavaDocLine(" * " + remarks);
            topLevelClass.addJavaDocLine(" * @date " + format.format(new Date()));
            topLevelClass.addJavaDocLine(" */");
        }
        topLevelClass.addImportedType(new FullyQualifiedJavaType("io.swagger.annotations.ApiModelProperty"));
        return true;
    }

    @Override
    public boolean modelRecordWithBLOBsClassGenerated(TopLevelClass topLevelClass,
            IntrospectedTable introspectedTable) {
        return true;
    }

    @Override
    public boolean validate(List<String> warnings) {
        return true;
    }

    private void closeConnection(Connection connection, ResultSet rs) {
        if (null != rs) {
            try {
                rs.close();
            } catch (SQLException e) {
            }
        }
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
            }
        }

    }
}
