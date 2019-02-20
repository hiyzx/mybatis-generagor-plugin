package org.mybatis;

import org.mybatis.dto.GeneratorProperties;
import org.mybatis.dto.TableProperties;
import org.mybatis.util.GeneratorUtil;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Properties;

/**
 * @author yezhaoxing
 * @since 2019/02/20
 */
public class GenerateMain {

    public static void main(String[] args) throws IOException {
        InputStream is = GenerateMain.class.getClassLoader().getResourceAsStream("my.properties");
        Properties config = new Properties();
        config.load(is);

        GeneratorProperties properties = new GeneratorProperties();
        // 数据库相关
        properties.setJdbcUrl(config.getProperty("jdbcUrl"));
        properties.setJdbcUsername(config.getProperty("jdbcUsername"));
        properties.setJdbcPassword(config.getProperty("jdbcPassword"));
        properties.setJdbcClassDriverName(config.getProperty("jdbcClassDriverName"));

        properties.setAuthor(config.getProperty("author"));
        String projectPath = config.getProperty("projectPath");
        properties.setProjectPath(projectPath);
        // resources需这里先创建
        new File(projectPath + properties.getResourcePath()).mkdirs();
        properties.setPluginClass(config.getProperty("pluginClass"));
        // 通用类相关
        properties.setMyMapperFile(config.getProperty("myMapperFile"));
        properties.setAbstractService(config.getProperty("abstractService"));
        properties.setAbstractServiceImpl(config.getProperty("abstractServiceImpl"));

        // 基础包名
        properties.setServicePackage(config.getProperty("basePackage") + ".service");
        properties.setMapperPackage(config.getProperty("basePackage") + ".dao");
        properties.setModelPackage(config.getProperty("basePackage") + ".po");

        // 需要转换的表名和类名
        TableProperties orderDetailProperties1 = new TableProperties("user_wx_info", "UserWxInfo", null);
        TableProperties orderDetailProperties2 = new TableProperties("user_info", "UserInfo", null);
        TableProperties orderDetailProperties3 = new TableProperties("user_relation", "UserRelation", null);
        properties.setTablePropertiesList(
                Arrays.asList(orderDetailProperties2, orderDetailProperties1, orderDetailProperties3));
        new GeneratorUtil(properties).generator();
        Desktop.getDesktop().open(new File(projectPath));
    }
}
