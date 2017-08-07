package org.mybatis.plugin;

import org.apache.commons.lang.StringUtils;
import org.mybatis.generator.api.*;
import org.mybatis.generator.api.dom.java.*;
import org.mybatis.generator.api.dom.xml.Document;
import org.mybatis.generator.api.dom.xml.XmlElement;
import org.mybatis.generator.config.CommentGeneratorConfiguration;
import org.mybatis.generator.config.Context;
import org.mybatis.generator.internal.db.ConnectionFactory;
import org.mybatis.generator.internal.util.StringUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tk.mybatis.mapper.MapperException;
import tk.mybatis.mapper.generator.MapperCommentGenerator;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * @see https://github.com/shan-ren/mybatis-generator-plugins-auto-comments/blob
 *      /master/CommentPlugin.java
 */
public class SpringBootWithDataGeneratorPlugin extends PluginAdapter {
    private static final Logger LOG = LoggerFactory.getLogger(CommentPlugin.class);

    private Set<String> mappers = new HashSet();
    private boolean caseSensitive = false;
    private String beginningDelimiter = "";
    private String endingDelimiter = "";
    private String schema;
    private CommentGeneratorConfiguration commentCfg;

    public void setContext(Context context) {
        super.setContext(context);
        this.commentCfg = new CommentGeneratorConfiguration();
        this.commentCfg.setConfigurationType(MapperCommentGenerator.class.getCanonicalName());
    }

    public void setProperties(Properties properties) {
        super.setProperties(properties);
        String mappers = this.properties.getProperty("mappers");
        if (!StringUtility.stringHasValue(mappers)) {
            throw new MapperException("Mapper插件缺少必要的mappers属性!");
        } else {
            String[] caseSensitive = mappers.split(",");
            int beginningDelimiter = caseSensitive.length;

            String schema;
            for (int endingDelimiter = 0; endingDelimiter < beginningDelimiter; ++endingDelimiter) {
                schema = caseSensitive[endingDelimiter];
                this.mappers.add(schema);
            }

            String var7 = this.properties.getProperty("caseSensitive");
            if (StringUtility.stringHasValue(var7)) {
                this.caseSensitive = var7.equalsIgnoreCase("TRUE");
            }

            String var8 = this.properties.getProperty("beginningDelimiter");
            if (StringUtility.stringHasValue(var8)) {
                this.beginningDelimiter = var8;
            }

            this.commentCfg.addProperty("beginningDelimiter", this.beginningDelimiter);
            String var9 = this.properties.getProperty("endingDelimiter");
            if (StringUtility.stringHasValue(var9)) {
                this.endingDelimiter = var9;
            }

            this.commentCfg.addProperty("endingDelimiter", this.endingDelimiter);
            schema = this.properties.getProperty("schema");
            if (StringUtility.stringHasValue(schema)) {
                this.schema = schema;
            }

        }
    }

    @Override
    public boolean modelFieldGenerated(Field field, TopLevelClass topLevelClass, IntrospectedColumn introspectedColumn,
            IntrospectedTable introspectedTable, ModelClassType modelClassType) {
        if (introspectedColumn.isIdentity()) {
            field.addJavaDocLine("@Id");
            field.addJavaDocLine("@GeneratedValue(strategy = GenerationType.IDENTITY)");
        }
        String remark = introspectedColumn.getRemarks();
        if (StringUtils.isNotEmpty(remark)) {
            field.addJavaDocLine(String.format("@ApiModelProperty(value = \"%s\")", remark));
        }
        return true;
    }

    @Override
    public boolean modelGetterMethodGenerated(Method method, TopLevelClass topLevelClass,
            IntrospectedColumn introspectedColumn, IntrospectedTable introspectedTable, ModelClassType modelClassType) {
        return true;
    }

    @Override
    public boolean modelSetterMethodGenerated(Method method, TopLevelClass topLevelClass,
            IntrospectedColumn introspectedColumn, IntrospectedTable introspectedTable, ModelClassType modelClassType) {
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
        topLevelClass.addAnnotation(String.format("@Table(name = \"%s\")", table.getIntrospectedTableName()));
        topLevelClass.addSuperInterface(new FullyQualifiedJavaType("java.io.Serializable"));
        topLevelClass.addImportedType(new FullyQualifiedJavaType("io.swagger.annotations.ApiModelProperty"));
        topLevelClass.addImportedType(new FullyQualifiedJavaType("javax.persistence.GeneratedValue"));
        topLevelClass.addImportedType(new FullyQualifiedJavaType("javax.persistence.GenerationType"));
        topLevelClass.addImportedType(new FullyQualifiedJavaType("javax.persistence.Id"));
        topLevelClass.addImportedType(new FullyQualifiedJavaType("javax.persistence.Table"));
        topLevelClass.addImportedType(new FullyQualifiedJavaType("java.io.Serializable"));
        return true;
    }

    @Override
    public boolean modelRecordWithBLOBsClassGenerated(TopLevelClass topLevelClass,
            IntrospectedTable introspectedTable) {
        topLevelClass.addImportedType(new FullyQualifiedJavaType("io.swagger.annotations.ApiModelProperty"));
        return true;
    }

    @Override
    public boolean validate(List<String> warnings) {
        return true;
    }

    @Override
    public boolean clientCountByExampleMethodGenerated(Method method, Interface interfaze,
            IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean clientCountByExampleMethodGenerated(Method method, TopLevelClass topLevelClass,
            IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean clientDeleteByExampleMethodGenerated(Method method, Interface interfaze,
            IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean clientDeleteByExampleMethodGenerated(Method method, TopLevelClass topLevelClass,
            IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean clientDeleteByPrimaryKeyMethodGenerated(Method method, Interface interfaze,
            IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean clientDeleteByPrimaryKeyMethodGenerated(Method method, TopLevelClass topLevelClass,
            IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean clientInsertMethodGenerated(Method method, Interface interfaze,
            IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean clientInsertMethodGenerated(Method method, TopLevelClass topLevelClass,
            IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean clientGenerated(Interface interfaze, TopLevelClass topLevelClass,
            IntrospectedTable introspectedTable) {
        FullyQualifiedJavaType entityType = new FullyQualifiedJavaType(introspectedTable.getBaseRecordType());
        Iterator var5 = this.mappers.iterator();

        while (var5.hasNext()) {
            String mapper = (String) var5.next();
            interfaze.addImportedType(new FullyQualifiedJavaType(mapper));
            interfaze.addSuperInterface(new FullyQualifiedJavaType(mapper + "<" + entityType.getShortName() + ">"));
        }

        interfaze.addImportedType(entityType);
        return true;
    }

    @Override
    public boolean clientSelectByExampleWithBLOBsMethodGenerated(Method method, Interface interfaze,
            IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean clientSelectByExampleWithBLOBsMethodGenerated(Method method, TopLevelClass topLevelClass,
            IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean clientSelectByExampleWithoutBLOBsMethodGenerated(Method method, Interface interfaze,
            IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean clientSelectByExampleWithoutBLOBsMethodGenerated(Method method, TopLevelClass topLevelClass,
            IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean clientSelectByPrimaryKeyMethodGenerated(Method method, Interface interfaze,
            IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean clientSelectByPrimaryKeyMethodGenerated(Method method, TopLevelClass topLevelClass,
            IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean clientUpdateByExampleSelectiveMethodGenerated(Method method, Interface interfaze,
            IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean clientUpdateByExampleSelectiveMethodGenerated(Method method, TopLevelClass topLevelClass,
            IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean clientUpdateByExampleWithBLOBsMethodGenerated(Method method, Interface interfaze,
            IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean clientUpdateByExampleWithBLOBsMethodGenerated(Method method, TopLevelClass topLevelClass,
            IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean clientUpdateByExampleWithoutBLOBsMethodGenerated(Method method, Interface interfaze,
            IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean clientUpdateByExampleWithoutBLOBsMethodGenerated(Method method, TopLevelClass topLevelClass,
            IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean clientUpdateByPrimaryKeySelectiveMethodGenerated(Method method, Interface interfaze,
            IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean clientUpdateByPrimaryKeySelectiveMethodGenerated(Method method, TopLevelClass topLevelClass,
            IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean clientUpdateByPrimaryKeyWithBLOBsMethodGenerated(Method method, Interface interfaze,
            IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean clientUpdateByPrimaryKeyWithBLOBsMethodGenerated(Method method, TopLevelClass topLevelClass,
            IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean clientUpdateByPrimaryKeyWithoutBLOBsMethodGenerated(Method method, Interface interfaze,
            IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean clientUpdateByPrimaryKeyWithoutBLOBsMethodGenerated(Method method, TopLevelClass topLevelClass,
            IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean modelExampleClassGenerated(TopLevelClass topLevelClass, IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean modelPrimaryKeyClassGenerated(TopLevelClass topLevelClass, IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean sqlMapResultMapWithoutBLOBsElementGenerated(XmlElement element,
            IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean sqlMapCountByExampleElementGenerated(XmlElement element, IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean sqlMapDeleteByExampleElementGenerated(XmlElement element, IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean sqlMapDeleteByPrimaryKeyElementGenerated(XmlElement element, IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean sqlMapDocumentGenerated(Document document, IntrospectedTable introspectedTable) {
        return true;
    }

    @Override
    public boolean sqlMapExampleWhereClauseElementGenerated(XmlElement element, IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean sqlMapGenerated(GeneratedXmlFile sqlMap, IntrospectedTable introspectedTable) {
        return true;
    }

    @Override
    public boolean sqlMapInsertElementGenerated(XmlElement element, IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean sqlMapResultMapWithBLOBsElementGenerated(XmlElement element, IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean sqlMapSelectByExampleWithoutBLOBsElementGenerated(XmlElement element,
            IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean sqlMapSelectByExampleWithBLOBsElementGenerated(XmlElement element,
            IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean sqlMapSelectByPrimaryKeyElementGenerated(XmlElement element, IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean sqlMapUpdateByExampleSelectiveElementGenerated(XmlElement element,
            IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean sqlMapUpdateByExampleWithBLOBsElementGenerated(XmlElement element,
            IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean sqlMapUpdateByExampleWithoutBLOBsElementGenerated(XmlElement element,
            IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean sqlMapUpdateByPrimaryKeySelectiveElementGenerated(XmlElement element,
            IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean sqlMapUpdateByPrimaryKeyWithBLOBsElementGenerated(XmlElement element,
            IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean sqlMapUpdateByPrimaryKeyWithoutBLOBsElementGenerated(XmlElement element,
            IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean sqlMapInsertSelectiveElementGenerated(XmlElement element, IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean clientInsertSelectiveMethodGenerated(Method method, Interface interfaze,
            IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean clientInsertSelectiveMethodGenerated(Method method, TopLevelClass topLevelClass,
            IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public void initialized(IntrospectedTable introspectedTable) {
        super.initialized(introspectedTable);
    }

    @Override
    public boolean providerGenerated(TopLevelClass topLevelClass, IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean sqlMapBaseColumnListElementGenerated(XmlElement element, IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean sqlMapBlobColumnListElementGenerated(XmlElement element, IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean providerApplyWhereMethodGenerated(Method method, TopLevelClass topLevelClass,
            IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean providerCountByExampleMethodGenerated(Method method, TopLevelClass topLevelClass,
            IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean providerDeleteByExampleMethodGenerated(Method method, TopLevelClass topLevelClass,
            IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean providerInsertSelectiveMethodGenerated(Method method, TopLevelClass topLevelClass,
            IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean providerSelectByExampleWithBLOBsMethodGenerated(Method method, TopLevelClass topLevelClass,
            IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean providerSelectByExampleWithoutBLOBsMethodGenerated(Method method, TopLevelClass topLevelClass,
            IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean providerUpdateByExampleSelectiveMethodGenerated(Method method, TopLevelClass topLevelClass,
            IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean providerUpdateByExampleWithBLOBsMethodGenerated(Method method, TopLevelClass topLevelClass,
            IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean providerUpdateByExampleWithoutBLOBsMethodGenerated(Method method, TopLevelClass topLevelClass,
            IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean providerUpdateByPrimaryKeySelectiveMethodGenerated(Method method, TopLevelClass topLevelClass,
            IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean clientSelectAllMethodGenerated(Method method, Interface interfaze,
            IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean clientSelectAllMethodGenerated(Method method, TopLevelClass topLevelClass,
            IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean sqlMapSelectAllElementGenerated(XmlElement element, IntrospectedTable introspectedTable) {
        return false;
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
