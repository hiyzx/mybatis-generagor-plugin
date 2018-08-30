package org.mybatis.util;

import freemarker.template.TemplateExceptionHandler;
import org.mybatis.dto.ColumnOverrideDto;
import org.mybatis.dto.GeneratorProperties;
import org.mybatis.dto.TableProperties;
import org.mybatis.generator.api.MyBatisGenerator;
import org.mybatis.generator.config.*;
import org.mybatis.generator.internal.DefaultShellCallback;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

/**
 * @author yezhaoxing
 * @since 2018/08/30
 */
public class GeneratorUtil {

    private GeneratorProperties properties;

    public GeneratorUtil(GeneratorProperties properties) {
        this.properties = properties;
    }

    public void generator() {
        for (TableProperties tableProperties : properties.getTablePropertiesList()) {
            // 根据需求生成，不需要的注掉，模板有问题的话可以自己修改。
            genModelAndMapper(tableProperties);
            genService(tableProperties.getTableName());
        }
    }

    private void genModelAndMapper(TableProperties tableProperties) {
        Context context = new Context(ModelType.FLAT);
        context.setId("mysql");
        context.setTargetRuntime("MyBatis3Simple");
        context.addProperty(PropertyRegistry.CONTEXT_BEGINNING_DELIMITER, "`");
        context.addProperty(PropertyRegistry.CONTEXT_ENDING_DELIMITER, "`");

        JDBCConnectionConfiguration jdbcConnectionConfiguration = new JDBCConnectionConfiguration();
        jdbcConnectionConfiguration.setConnectionURL(properties.getJdbcUrl());
        jdbcConnectionConfiguration.setUserId(properties.getJdbcUsername());
        jdbcConnectionConfiguration.setPassword(properties.getJdbcPassword());
        jdbcConnectionConfiguration.setDriverClass(properties.getJdbcClassDriverName());
        context.setJdbcConnectionConfiguration(jdbcConnectionConfiguration);

        CommentGeneratorConfiguration commentGeneratorConfiguration = new CommentGeneratorConfiguration();
        commentGeneratorConfiguration.addProperty("suppressDate", "true");
        commentGeneratorConfiguration.addProperty("suppressAllComments", "true");
        context.setCommentGeneratorConfiguration(commentGeneratorConfiguration);

        PluginConfiguration pluginConfiguration = new PluginConfiguration();
        pluginConfiguration.setConfigurationType(properties.getPluginClass());
        pluginConfiguration.addProperty("mappers", properties.getMyMapperFile());
        context.addPluginConfiguration(pluginConfiguration);

        JavaModelGeneratorConfiguration javaModelGeneratorConfiguration = new JavaModelGeneratorConfiguration();
        javaModelGeneratorConfiguration.setTargetProject(properties.getProjectPath() + properties.getJavaPath());
        javaModelGeneratorConfiguration.setTargetPackage(properties.getModelPackage());
        context.setJavaModelGeneratorConfiguration(javaModelGeneratorConfiguration);

        SqlMapGeneratorConfiguration sqlMapGeneratorConfiguration = new SqlMapGeneratorConfiguration();
        sqlMapGeneratorConfiguration.setTargetProject(properties.getProjectPath() + properties.getResourcePath());
        sqlMapGeneratorConfiguration.setTargetPackage(properties.getMapperPackage());
        context.setSqlMapGeneratorConfiguration(sqlMapGeneratorConfiguration);

        JavaClientGeneratorConfiguration javaClientGeneratorConfiguration = new JavaClientGeneratorConfiguration();
        javaClientGeneratorConfiguration.setTargetProject(properties.getProjectPath() + properties.getJavaPath());
        javaClientGeneratorConfiguration.setTargetPackage(properties.getMapperPackage());
        javaClientGeneratorConfiguration.setConfigurationType("XMLMAPPER");
        context.setJavaClientGeneratorConfiguration(javaClientGeneratorConfiguration);

        TableConfiguration tableConfiguration = new TableConfiguration(context);
        tableConfiguration.setTableName(tableProperties.getTableName());
        tableConfiguration.setDomainObjectName(tableProperties.getTableClassName());
        tableConfiguration.setGeneratedKey(new GeneratedKey("id", "Mysql", true, null));
        for (ColumnOverrideDto columnOverrideDto : tableProperties.getColumnOverrides()) {
            ColumnOverride columnOverride = new ColumnOverride(columnOverrideDto.getColumnName());
            columnOverride.setJavaProperty(columnOverrideDto.getJavaProperty());
            columnOverride.setJavaType(columnOverrideDto.getJavaType());
            tableConfiguration.addColumnOverride(columnOverride);
        }
        context.addTableConfiguration(tableConfiguration);

        List<String> warnings;
        MyBatisGenerator generator;
        try {
            Configuration config = new Configuration();
            config.addContext(context);
            config.validate();

            boolean overwrite = true;
            DefaultShellCallback callback = new DefaultShellCallback(overwrite);
            warnings = new ArrayList<>();
            generator = new MyBatisGenerator(config, callback, warnings);
            generator.generate(null);
        } catch (Exception e) {
            throw new RuntimeException("生成Model和Mapper失败", e);
        }

        if (generator.getGeneratedJavaFiles().isEmpty() || generator.getGeneratedXmlFiles().isEmpty()) {
            throw new RuntimeException("生成Model和Mapper失败：" + warnings);
        }

        String modelName = tableNameConvertUpperCamel(tableProperties.getTableClassName());
        System.out.println(modelName + ".java 生成成功");
        System.out.println(modelName + "Mapper.java 生成成功");
        System.out.println(modelName + "Mapper.xml 生成成功");
    }

    private void genService(String tableName) {
        try {
            freemarker.template.Configuration cfg = getConfiguration();

            Map<String, Object> data = new HashMap<>();
            data.put("date", properties.getDATE());
            data.put("author", properties.getAuthor());
            String modelNameUpperCamel = tableNameConvertUpperCamel(tableName);
            data.put("modelNameUpperCamel", modelNameUpperCamel);
            data.put("modelNameLowerCamel", tableNameConvertLowerCamel(tableName));
            data.put("servicePackage", properties.getServicePackage());
            data.put("mapperPackage", properties.getMapperPackage());
            data.put("modelPackage", properties.getModelPackage());
            data.put("abstractService", properties.getAbstractService());
            data.put("abstractServiceImpl", properties.getAbstractServiceImpl());

            String baseServicePath = properties.getProjectPath() + properties.getJavaPath()
                    + packageConvertPath(properties.getServicePackage());

            File file = new File(baseServicePath + modelNameUpperCamel + "Service.java");
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
            cfg.getTemplate("service.ftl").process(data, new FileWriter(file));
            System.out.println(modelNameUpperCamel + "Service.java 生成成功");

            File file1 = new File(baseServicePath + "/impl/" + modelNameUpperCamel + "ServiceImpl.java");
            if (!file1.getParentFile().exists()) {
                file1.getParentFile().mkdirs();
            }
            cfg.getTemplate("service-impl.ftl").process(data, new FileWriter(file1));
            System.out.println(modelNameUpperCamel + "ServiceImpl.java 生成成功");
        } catch (Exception e) {
            throw new RuntimeException("生成Service失败", e);
        }
    }

    private freemarker.template.Configuration getConfiguration() throws IOException {
        freemarker.template.Configuration cfg = new freemarker.template.Configuration(
                freemarker.template.Configuration.VERSION_2_3_23);
        cfg.setDirectoryForTemplateLoading(new File(properties.getProjectPath() + properties.getTemplatePath()));
        cfg.setDefaultEncoding("UTF-8");
        cfg.setTemplateExceptionHandler(TemplateExceptionHandler.IGNORE_HANDLER);
        return cfg;
    }

    private String tableNameConvertLowerCamel(String tableName) {
        StringBuilder result = new StringBuilder();
        if (tableName != null && tableName.length() > 0) {
            tableName = tableName.toLowerCase();// 兼容使用大写的表名
            boolean flag = false;
            for (int i = 0; i < tableName.length(); i++) {
                char ch = tableName.charAt(i);
                if ("_".charAt(0) == ch) {
                    flag = true;
                } else {
                    if (flag) {
                        result.append(Character.toUpperCase(ch));
                        flag = false;
                    } else {
                        result.append(ch);
                    }
                }
            }
        }
        return result.toString();
    }

    private String tableNameConvertUpperCamel(String tableName) {
        String camel = tableNameConvertLowerCamel(tableName);
        return camel.substring(0, 1).toUpperCase() + camel.substring(1);

    }

    private static String packageConvertPath(String packageName) {
        return String.format("/%s/", packageName.contains(".") ? packageName.replaceAll("\\.", "/") : packageName);
    }

    public static void main(String[] args) {
        GeneratorProperties properties = new GeneratorProperties();
        properties.setJdbcUrl(
                "jdbc:mysql://");
        properties.setJdbcUsername("root");
        properties.setJdbcPassword("pass");
        properties.setJdbcClassDriverName("com.mysql.jdbc.Driver");
        properties.setAuthor("zero");
        properties.setProjectPath("E:/app/" + "takeaway/api/product-service/");
        properties.setPluginClass("org.mybatis.plugin.SpringBootGeneratorPlugin");
        properties.setMyMapperFile("com.zero.common.conf.MyMapper");
        properties.setServicePackage("com.zero.product.service");
        properties.setMapperPackage("com.zero.product.dao");
        properties.setModelPackage("com.zero.product.po");
        properties.setAbstractService("com.zero.common.common.BaseService");
        properties.setAbstractServiceImpl("com.zero.common.common.AbstractService");
        TableProperties orderDetailProperties = new TableProperties("order_detail", "null", null);
        properties.setTablePropertiesList(Arrays.asList(orderDetailProperties));
        new GeneratorUtil(properties).generator();
    }
}
