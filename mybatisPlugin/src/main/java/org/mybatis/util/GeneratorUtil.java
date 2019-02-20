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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        Context context = context();
        for (TableProperties tableProperties : properties.getTablePropertiesList()) {
            // 根据需求生成，不需要的注掉，模板有问题的话可以自己修改。
            genModelAndMapper(context, tableProperties);
            genService(tableProperties.getTableName());
        }
    }

    private Context context() {
        Context context = new Context(ModelType.FLAT);
        context.setId("mysql");// oracle or mysql
        context.setTargetRuntime("MyBatis3Simple");
        context.addProperty(PropertyRegistry.CONTEXT_BEGINNING_DELIMITER, "`");
        context.addProperty(PropertyRegistry.CONTEXT_ENDING_DELIMITER, "`");

        // 数据库链接URL，用户名、密码
        JDBCConnectionConfiguration jdbcConnectionConfiguration = new JDBCConnectionConfiguration();
        jdbcConnectionConfiguration.setConnectionURL(properties.getJdbcUrl());
        jdbcConnectionConfiguration.setUserId(properties.getJdbcUsername());
        jdbcConnectionConfiguration.setPassword(properties.getJdbcPassword());
        jdbcConnectionConfiguration.setDriverClass(properties.getJdbcClassDriverName());
        jdbcConnectionConfiguration.addProperty("remarksReporting", "true");// 针对oracle
        context.setJdbcConnectionConfiguration(jdbcConnectionConfiguration);

        CommentGeneratorConfiguration commentGeneratorConfiguration = new CommentGeneratorConfiguration();
        commentGeneratorConfiguration.addProperty("suppressDate", "true");
        commentGeneratorConfiguration.addProperty("suppressAllComments", "true");
        context.setCommentGeneratorConfiguration(commentGeneratorConfiguration);

        PluginConfiguration pluginConfiguration = new PluginConfiguration();
        pluginConfiguration.setConfigurationType(properties.getPluginClass());
        pluginConfiguration.addProperty("mappers", properties.getMyMapperFile());
        context.addPluginConfiguration(pluginConfiguration);

        // 生成模型的包名和位置
        JavaModelGeneratorConfiguration javaModelGeneratorConfiguration = new JavaModelGeneratorConfiguration();
        javaModelGeneratorConfiguration.setTargetProject(properties.getProjectPath() + properties.getJavaPath());
        javaModelGeneratorConfiguration.setTargetPackage(properties.getModelPackage());
        javaModelGeneratorConfiguration.addProperty("enableSubPackages", "true");
        javaModelGeneratorConfiguration.addProperty("trimStrings", "true");
        context.setJavaModelGeneratorConfiguration(javaModelGeneratorConfiguration);

        // 生成映射文件的包名和位置
        SqlMapGeneratorConfiguration sqlMapGeneratorConfiguration = new SqlMapGeneratorConfiguration();
        sqlMapGeneratorConfiguration.setTargetProject(properties.getProjectPath() + properties.getResourcePath());
        sqlMapGeneratorConfiguration.setTargetPackage(properties.getMapperPackage());
        sqlMapGeneratorConfiguration.addProperty("enableSubPackages", "true");
        context.setSqlMapGeneratorConfiguration(sqlMapGeneratorConfiguration);

        // 生成DAO的包名和位置
        JavaClientGeneratorConfiguration javaClientGeneratorConfiguration = new JavaClientGeneratorConfiguration();
        javaClientGeneratorConfiguration.setTargetProject(properties.getProjectPath() + properties.getJavaPath());
        javaClientGeneratorConfiguration.setTargetPackage(properties.getMapperPackage());
        javaClientGeneratorConfiguration.setConfigurationType("XMLMAPPER");
        javaClientGeneratorConfiguration.addProperty("enableSubPackages", "true");
        context.setJavaClientGeneratorConfiguration(javaClientGeneratorConfiguration);
        return context;
    }

    private void genModelAndMapper(Context context, TableProperties tableProperties) {
        // 要生成的表
        TableConfiguration tableConfiguration = new TableConfiguration(context);
        tableConfiguration.setTableName(tableProperties.getTableName());
        tableConfiguration.setDomainObjectName(tableProperties.getTableClassName());
        tableConfiguration.setGeneratedKey(new GeneratedKey("id", "Mysql", true, null));
        List<ColumnOverride> columnOverrides = tableConfiguration.getColumnOverrides();
        if (!columnOverrides.isEmpty()) {
            for (ColumnOverrideDto columnOverrideDto : tableProperties.getColumnOverrides()) {
                ColumnOverride columnOverride = new ColumnOverride(columnOverrideDto.getColumnName());
                columnOverride.setJavaProperty(columnOverrideDto.getJavaProperty());
                columnOverride.setJavaType(columnOverrideDto.getJavaType());
                tableConfiguration.addColumnOverride(columnOverride);
            }
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

        String modelName = tableProperties.getTableClassName();
        System.out.println(modelName + ".java 生成成功");
        System.out.println(modelName + "Mapper.java 生成成功");
        System.out.println(modelName + "Mapper.xml 生成成功");
    }

    // 生成service
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

            String baseServicePath = properties.getProjectPath() + properties.getJavaPath() + packageConvertPath(
                    properties.getServicePackage());

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
        cfg.setDirectoryForTemplateLoading(new File(
                "E:\\app\\mybatis-generagor-plugin\\mybatisPlugin\\src\\main\\resources\\generator\\template"));
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
}
